USE developer_workspace;

-- JOIN + aggregate: task count per project
SELECT p.name, COUNT(t.task_id) AS task_count
FROM projects p
LEFT JOIN tasks t ON p.project_id = t.project_id
GROUP BY p.project_id, p.name;

-- CTE + DENSE_RANK: rank projects by number of completed tasks
WITH completed AS (
    SELECT p.project_id, p.name, COUNT(t.task_id) AS completed_tasks
    FROM projects p
    LEFT JOIN tasks t
      ON p.project_id = t.project_id
     AND t.status = 'COMPLETED'
    GROUP BY p.project_id, p.name
)
SELECT name, completed_tasks,
       DENSE_RANK() OVER (ORDER BY completed_tasks DESC) AS project_rank
FROM completed;

-- LAG: compare each coding session with the previous session
SELECT session_id, start_time, duration_seconds,
       LAG(duration_seconds) OVER (ORDER BY start_time) AS previous_duration
FROM coding_sessions
WHERE user_id = 1;

-- LEAD: next coding session
SELECT session_id, start_time,
       LEAD(start_time) OVER (ORDER BY start_time) AS next_session
FROM coding_sessions
WHERE user_id = 1;

-- RANK: rank DSA topics by solved problems
SELECT dt.name,
       SUM(CASE WHEN dp.solved THEN 1 ELSE 0 END) AS solved,
       RANK() OVER (
           ORDER BY SUM(CASE WHEN dp.solved THEN 1 ELSE 0 END) DESC
       ) AS topic_rank
FROM dsa_topics dt
LEFT JOIN dsa_problems dp ON dt.topic_id = dp.topic_id
WHERE dt.user_id = 1
GROUP BY dt.topic_id, dt.name;
