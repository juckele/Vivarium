/*
 * Copyright © 2015 John H Uckele. All rights reserved.
 */

package io.vivarium.server.workloadmanagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;

import io.vivarium.persistence.JobModel;
import io.vivarium.persistence.JobStatus;
import io.vivarium.persistence.PersistenceModule;
import io.vivarium.persistence.WorkerModel;
import io.vivarium.server.ClientConnectionManager;
import io.vivarium.util.UUID;
import io.vivarium.util.concurrency.VoidFunction;

public class WorkloadEnforcer implements VoidFunction
{
    // static configs
    public final static long DEFAULT_ENFORCE_TIME_GAP_IN_MS = 60_000;

    // Dependencies
    private final PersistenceModule _persistenceModule;
    private final ClientConnectionManager _clientConnectionManager;

    public WorkloadEnforcer(PersistenceModule persistenceModule, ClientConnectionManager clientConnectionManager)
    {
        _persistenceModule = persistenceModule;
        _clientConnectionManager = clientConnectionManager;
    }

    @Override
    public void execute()
    {
        // Update the job statuses
        _persistenceModule.updateJobStatuses();

        // Get the current worker models
        List<WorkerModel> workers = _persistenceModule.fetchAllWorkers();
        // @formatter:off
        List<WorkerModel> activeWorkers = workers.stream()
                .filter(w -> w.isActive())
                .collect(Collectors.toList());
        // @formatter:on

        // Get the top priority unblocked jobs, the currently assigned jobs
        List<JobModel> waitingJobs = _persistenceModule.fetchJobsWithStatus(JobStatus.WAITING);
        List<JobModel> assignedJobs = _persistenceModule.fetchJobsWithStatus(JobStatus.PROCESSING);
        List<JobModel> prioritySortedJobs = new ArrayList<>(waitingJobs);
        prioritySortedJobs.addAll(assignedJobs);
        prioritySortedJobs.sort(new JobPriorityComparator());

        // Determine optimal greedy allocation of jobs and the current real allocation
        JobAssingments idealAssingments = buildDesiredJobAssingments(activeWorkers, prioritySortedJobs);
        JobAssingments actualAssingments = buildActualJobAssingments(activeWorkers, assignedJobs);

        // Build plan to assign jobs (only) if doing so can improve the allocation score
        // TODO: IMPLEMENT

        // If reassigning jobs was not able to improve greedy allocation score, check to see if a single unassignment +
        // reassignment can improve the score. This will fail to find complex rearrangements which might exist, but it
        // is assumed that these would be marginal improvements at best.
        // TODO: IMPLEMENT

        // Send assignments and await acks from workers for assignments.
        // TODO: IMPLEMENT
    }

    private JobAssingments buildDesiredJobAssingments(Collection<WorkerModel> workers,
            List<JobModel> prioritySortedJobs)
    {
        JobAssingments desiredAssingments = new JobAssingments(workers);
        Map<UUID, WorkerModel> workersByID = Maps.uniqueIndex(workers, WorkerModel::getWorkerID);
        for (JobModel job : prioritySortedJobs)
        {
            long maxScoreImprovement = 0;
            WorkerModel assignToWorker = null;
            for (WorkerModel worker : workers)
            {
                long workerScoreImprovment = desiredAssingments.getScoreChangeForJob(worker, job.getPriority());
                if (workerScoreImprovment > maxScoreImprovement)
                {
                    maxScoreImprovement = workerScoreImprovment;
                    assignToWorker = worker;
                }
            }
            if (assignToWorker != null)
            {
                desiredAssingments.addWorkerJob(assignToWorker, job.getPriority());
            }
        }
        return desiredAssingments;
    }

    private JobAssingments buildActualJobAssingments(Collection<WorkerModel> workers, List<JobModel> assignedJobs)
    {
        JobAssingments actualAssingments = new JobAssingments(workers);
        Map<UUID, WorkerModel> workersByID = Maps.uniqueIndex(workers, WorkerModel::getWorkerID);
        for (JobModel job : assignedJobs)
        {
            UUID workerID = job.getCheckedOutByWorkerID().get();
            actualAssingments.addWorkerJob(workersByID.get(workerID), job.getPriority());
        }
        return actualAssingments;
    }

    private static class JobAssingments
    {
        private Map<WorkerModel, Integer> _workerJobCounts = new HashMap<>();
        private Map<WorkerModel, Map<Integer, Integer>> _workerJobPriorityCounts = new HashMap<>();
        private Map<WorkerModel, Long> _workerScores = new HashMap<>();

        public JobAssingments(Collection<WorkerModel> workers)
        {
            for (WorkerModel worker : workers)
            {
                _workerJobCounts.put(worker, 0);
                _workerJobPriorityCounts.put(worker, new HashMap<>());
                _workerScores.put(worker, 0L);
            }
        }

        public long getScoreChangeForJob(WorkerModel workerModel, int priority)
        {
            // Figure out the current job count for this worker
            int workerJobCount = _workerJobCounts.get(workerModel);

            // Figure out the new score
            long newScore = determineWorkerScore(workerModel, workerJobCount + 1);
            long throughput = workerModel.getThroughputs()[workerJobCount];
            long throughputPerJob = throughput / workerJobCount;
            newScore += throughputPerJob * priority;

            // Determine how the score has changed
            long scoreChange = newScore - _workerScores.get(workerModel);
            return scoreChange;
        }

        public void addWorkerJob(WorkerModel workerModel, int priority)
        {
            // Update the job count for this worker
            int workerJobCount = _workerJobCounts.get(workerModel);
            workerJobCount++;
            _workerJobCounts.put(workerModel, workerJobCount);

            // Update the job count for this worker and priority
            int workerPriorityJobCount = _workerJobPriorityCounts.get(workerModel).get(priority);
            workerPriorityJobCount++;
            _workerJobPriorityCounts.get(workerModel).put(priority, workerPriorityJobCount);

            // Update the score
            long newScore = determineWorkerScore(workerModel, workerJobCount);
            _workerScores.put(workerModel, newScore);
        }

        private long determineWorkerScore(WorkerModel workerModel, int workerJobCount)
        {
            long throughput = workerModel.getThroughputs()[workerJobCount - 1];
            long throughputPerJob = throughput / workerJobCount;
            long workerScore = 0;
            Map<Integer, Integer> jobPriorityCounts = _workerJobPriorityCounts.get(workerModel);
            for (int priorityLevel : jobPriorityCounts.keySet())
            {
                workerScore += jobPriorityCounts.get(priorityLevel) * throughputPerJob * priorityLevel;
            }

            return workerScore;
        }

        public long getScore()
        {
            long totalScore = 0;
            for (long workerScore : _workerScores.values())
            {
                totalScore += workerScore;
            }
            return totalScore;
        }
    }

    private static class JobPriorityComparator implements Comparator<JobModel>
    {
        @Override
        public int compare(JobModel o1, JobModel o2)
        {
            if (o1.getPriority() < o2.getPriority())
            {
                return 1;
            }
            else if (o1.getPriority() > o2.getPriority())
            {
                return -1;
            }
            else
            {
                return 0;
            }
        }
    }
}