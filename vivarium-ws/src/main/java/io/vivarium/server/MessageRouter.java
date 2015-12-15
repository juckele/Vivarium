/*
 * Copyright © 2015 John H Uckele. All rights reserved.
 */

package io.vivarium.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.gwtstreamer.client.Streamer;

import io.vivarium.net.jobs.CreateWorldJob;
import io.vivarium.net.jobs.SimulationJob;
import io.vivarium.net.messages.CreateJobMessage;
import io.vivarium.net.messages.Message;
import io.vivarium.net.messages.RequestResourceMessage;
import io.vivarium.net.messages.ResourceFormat;
import io.vivarium.net.messages.SendResourceMessage;
import io.vivarium.net.messages.WorkerPledgeMessage;
import io.vivarium.persistence.model.CreateWorldJobModel;
import io.vivarium.persistence.model.JobModel;
import io.vivarium.persistence.model.JobStatus;
import io.vivarium.persistence.model.ResourceModel;
import io.vivarium.persistence.model.RunSimulationJobModel;
import io.vivarium.persistence.model.WorkerModel;
import io.vivarium.serialization.JSONConverter;
import io.vivarium.serialization.VivariumObjectCollection;
import io.vivarium.util.UUID;
import io.vivarium.util.Version;

public class MessageRouter
{
    private final Connection _databaseConnection;
    private final ClientConnectionManager _connectionManager;
    private final WorkloadManager _workloadManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public MessageRouter(Connection databaseConnection, ClientConnectionManager connectionManager,
            WorkloadManager workloadManager)
    {
        _databaseConnection = databaseConnection;
        _connectionManager = connectionManager;
        _workloadManager = workloadManager;
    }

    public void onOpen(WebSocket conn, ClientHandshake handshake)
    {
        System.out.println("SERVER: Web Socket Connection Opened. " + conn + " ~ " + handshake);
    }

    public void onClose(WebSocket conn, int code, String reason, boolean remote)
    {
        System.out.println(
                "SERVER: Web Socket Connection closed. " + conn + " ~ " + code + " # " + reason + " & " + remote);
    }

    public void onMessage(WebSocket conn, String message)
    {
        try
        {
            Message untypedMessage = mapper.readValue(message, Message.class);
            if (untypedMessage instanceof WorkerPledgeMessage)
            {
                acceptPledge(conn, (WorkerPledgeMessage) untypedMessage);
            }
            else if (untypedMessage instanceof SendResourceMessage)
            {
                acceptResource(conn, (SendResourceMessage) untypedMessage);
            }
            else if (untypedMessage instanceof RequestResourceMessage)
            {
                handleRequestForResource(conn, (RequestResourceMessage) untypedMessage);
            }
            else if (untypedMessage instanceof CreateJobMessage)
            {
                System.out.println("CreateJobMessage: " + message);
                acceptJob(conn, (CreateJobMessage) untypedMessage);
            }
            else
            {
                System.err.println("SERVER: Unhandled message of type " + untypedMessage.getClass().getSimpleName());
            }
        }
        catch (IOException | SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(
                "SERVER: Web Socket Message . " + conn + " ~ " + message.substring(0, Math.min(message.length(), 200)));
    }

    private synchronized void acceptPledge(WebSocket webSocket, WorkerPledgeMessage pledge) throws SQLException
    {
        WorkerModel worker = new WorkerModel(pledge.workerID, pledge.throughputs, pledge.active, new Date(),
                pledge.fileFormatVersion, pledge.codeVersion);
        worker.persistToDatabase(_databaseConnection);
        _connectionManager.registerWorker(pledge.workerID, webSocket);
    }

    private void acceptResource(WebSocket webSocket, SendResourceMessage sendResourceMessage) throws SQLException
    {
        String dataString = sendResourceMessage.dataString;
        String jsonString;
        if (sendResourceMessage.resourceFormat == ResourceFormat.JSON)
        {
            jsonString = sendResourceMessage.dataString;
        }
        else if (sendResourceMessage.resourceFormat == ResourceFormat.GWT_STREAM)
        {
            VivariumObjectCollection collection = (VivariumObjectCollection) Streamer.get().fromString(dataString);
            jsonString = JSONConverter.serializerToJSONString(collection);
        }
        else
        {
            throw new IllegalStateException("Unexpected resource format " + sendResourceMessage.resourceFormat);
        }
        ResourceModel resource = new ResourceModel(sendResourceMessage.resourceID, jsonString,
                Version.FILE_FORMAT_VERSION);
        resource.persistToDatabase(_databaseConnection);
    }

    private void acceptJob(WebSocket conn, CreateJobMessage createJobMessage) throws SQLException
    {
        JobModel job;
        if (createJobMessage.job instanceof SimulationJob)
        {
            SimulationJob simulationJob = (SimulationJob) createJobMessage.job;
            job = new RunSimulationJobModel(simulationJob.jobID, JobStatus.BLOCKED, (short) 0, null, null, null,
                    simulationJob.endTick, simulationJob.inputResources, simulationJob.outputResources,
                    simulationJob.dependencies);
        }
        else if (createJobMessage.job instanceof CreateWorldJob)
        {
            CreateWorldJob createWorldJob = (CreateWorldJob) createJobMessage.job;
            job = new CreateWorldJobModel(createWorldJob.jobID, JobStatus.BLOCKED, (short) 0, null, null, null,
                    createWorldJob.inputResources, createWorldJob.outputResources, createWorldJob.dependencies);
        }
        else
        {
            throw new IllegalStateException("Unexpected job type " + createJobMessage.job.getClass().getSimpleName());
        }
        job.persistToDatabase(_databaseConnection);
    }

    private void handleRequestForResource(WebSocket webSocket, RequestResourceMessage requestResourceMessage)
            throws SQLException, IOException
    {
        UUID resourceID = requestResourceMessage.resourceID;
        Optional<ResourceModel> resource = ResourceModel.getFromDatabase(_databaseConnection, resourceID);
        if (resource.isPresent() && resource.get().jsonData.isPresent())
        {
            ResourceFormat resourceFormat = requestResourceMessage.resourceFormat;
            String jsonString = resource.get().jsonData.get();
            String dataString = null;
            if (resourceFormat == ResourceFormat.JSON)
            {
                dataString = jsonString;
            }
            else if (resourceFormat == ResourceFormat.GWT_STREAM)
            {
                VivariumObjectCollection collection = JSONConverter.jsonStringToSerializerCollection(jsonString);
                dataString = Streamer.get().toString(collection);
            }
            else
            {
                throw new IllegalStateException("Unexpected resource format " + resourceFormat);
            }
            SendResourceMessage response = new SendResourceMessage(requestResourceMessage.resourceID, dataString,
                    resourceFormat);
            webSocket.send(mapper.writeValueAsString(response));
        }
    }

    public void onError(WebSocket conn, Exception ex)
    {
        System.out.println("SERVER: Web Socket Error . " + conn + " ~ " + ex);
        ex.printStackTrace();
    }
}