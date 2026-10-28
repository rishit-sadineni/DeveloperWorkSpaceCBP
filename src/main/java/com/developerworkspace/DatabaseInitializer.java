package com.developerworkspace;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public final class DatabaseInitializer {
    private DatabaseInitializer() {}

    public static void initialize() throws AppException {
        try (Connection server = DBConnection.getServerConnection();
             Statement st = server.createStatement()) {

            st.executeUpdate("CREATE DATABASE IF NOT EXISTS developer_workspace");

            try (Connection con = DBConnection.getConnection();
                 Statement s = con.createStatement()) {

                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id INT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(50) NOT NULL UNIQUE,
                        password_hash VARCHAR(64) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS projects (
                        project_id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        description VARCHAR(500),
                        technologies VARCHAR(300),
                        status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
                        github_url VARCHAR(300),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    )
                """);

                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        task_id INT PRIMARY KEY AUTO_INCREMENT,
                        project_id INT NOT NULL,
                        title VARCHAR(150) NOT NULL,
                        priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
                        status VARCHAR(30) NOT NULL DEFAULT 'TODO',
                        due_date DATE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
                    )
                """);

                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS snippets (
                        snippet_id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        title VARCHAR(150) NOT NULL,
                        language VARCHAR(40) NOT NULL,
                        tags VARCHAR(300),
                        code TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    )
                """);

                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dsa_topics (
                        topic_id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        name VARCHAR(80) NOT NULL,
                        UNIQUE(user_id, name),
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    )
                """);

                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dsa_problems (
                        problem_id INT PRIMARY KEY AUTO_INCREMENT,
                        topic_id INT NOT NULL,
                        title VARCHAR(150) NOT NULL,
                        difficulty VARCHAR(20) NOT NULL,
                        platform VARCHAR(100),
                        url VARCHAR(500),
                        solved BOOLEAN NOT NULL DEFAULT FALSE,
                        FOREIGN KEY (topic_id) REFERENCES dsa_topics(topic_id) ON DELETE CASCADE
                    )
                """);
                ensureDsaProblemColumns(con);

                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        bookmark_id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        title VARCHAR(150) NOT NULL,
                        url VARCHAR(500) NOT NULL,
                        category VARCHAR(80),
                        tags VARCHAR(300),
                        description VARCHAR(500),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                    )
                """);

                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS coding_sessions (
                        session_id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        project_id INT,
                        start_time DATETIME NOT NULL,
                        end_time DATETIME NOT NULL,
                        duration_seconds INT NOT NULL,
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                        FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE SET NULL
                    )
                """);

                createDefaultUser(con);
                createDefaultTopics(con);
                createDemoData(con);
            }
        } catch (SQLException e) {
            throw new AppException(
                    "Database initialization failed. Check MySQL is running and DBConfig credentials are correct.",
                    e
            );
        }
    }

    private static void createDefaultUser(Connection con) throws SQLException {
        String sql = "INSERT IGNORE INTO users(username, password_hash) VALUES(?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.sha256("admin123"));
            ps.executeUpdate();
        }
    }

    private static void ensureDsaProblemColumns(Connection con) throws SQLException {
        addColumnIfMissing(con, "platform", "VARCHAR(100)");
        addColumnIfMissing(con, "url", "VARCHAR(500)");
    }

    private static void addColumnIfMissing(Connection con, String column, String definition)
            throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name='dsa_problems' AND column_name=?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (Statement st = con.createStatement()) {
                        st.executeUpdate("ALTER TABLE dsa_problems ADD COLUMN " + column + " " + definition);
                    }
                }
            }
        }
    }

    private static void createDefaultTopics(Connection con) throws SQLException {
        int userId;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT user_id FROM users WHERE username=?")) {
            ps.setString(1, "admin");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                userId = rs.getInt(1);
            }
        }

        String[] topics = {"Arrays", "Strings", "Linked List", "Stack",
                "Queue", "Trees", "Graphs", "Dynamic Programming"};

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT IGNORE INTO dsa_topics(user_id, name) VALUES(?, ?)")) {
            for (String topic : topics) {
                ps.setInt(1, userId);
                ps.setString(2, topic);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void createDemoData(Connection con) throws SQLException {
        int userId = findUserId(con, "admin");
        if (userId == 0) return;

        String[] projectNames = {
                "FocusFlow Planner", "PantryPal Mobile", "Campus Events Hub",
                "LogLens Dashboard", "StudySprint API", "GreenRoute",
                "InvoiceCraft"
        };
        int[] projectIds = new int[projectNames.length];
        String[] descriptions = {
                "A distraction-aware weekly planner for balancing deep work and personal goals.",
                "A mobile-first meal planner that builds recipes from ingredients already at home.",
                "A searchable events platform for student clubs, workshops, and community meetups.",
                "A lightweight observability dashboard for filtering application logs and alerts.",
                "A REST API for study plans, spaced repetition sessions, and progress summaries.",
                "A route planner that compares walking, cycling, and public transport emissions.",
                "A small business invoicing tool with recurring billing and payment reminders."
        };
        String[] technologies = {
                "React, Spring Boot, PostgreSQL", "Flutter, Firebase, Figma",
                "Next.js, TypeScript, Redis", "Vue, Node.js, Elasticsearch",
                "Java, Spring Boot, MySQL", "Python, FastAPI, OpenStreetMap",
                "Java, Swing, MySQL"
        };
        String[] statuses = {"IN PROGRESS", "PLANNED", "IN PROGRESS", "PLANNED",
                "IN PROGRESS", "PLANNED", "COMPLETED"};
        String[] githubUrls = {
                "https://github.com/demo/focusflow-planner",
                "https://github.com/demo/pantrypal-mobile",
                "https://github.com/demo/campus-events-hub",
                "https://github.com/demo/loglens-dashboard",
                "https://github.com/demo/studysprint-api",
                "https://github.com/demo/greenroute",
                "https://github.com/demo/invoicecraft"
        };

        for (int i = 0; i < projectNames.length; i++) {
            projectIds[i] = findOrCreateProject(con, userId, projectNames[i],
                    descriptions[i], technologies[i], statuses[i], githubUrls[i]);
        }

        String[] taskTitles = {
                "Add drag-and-drop calendar view", "Create pantry barcode scanner",
                "Review event submission validation", "Add severity filters to log table",
                "Document authentication endpoints", "Compare route emission estimates",
                "Verify invoice PDF export"
        };
        String[] priorities = {"HIGH", "MEDIUM", "HIGH", "MEDIUM", "LOW", "MEDIUM", "HIGH"};
        String[] taskStatuses = {"IN PROGRESS", "TODO", "TODO", "IN PROGRESS",
                "COMPLETED", "TODO", "COMPLETED"};
        String[] dueDates = {"2026-11-05", "2026-11-12", "2026-10-30", "2026-11-08",
                "2026-10-20", "2026-11-18", "2026-10-15"};
        for (int i = 0; i < taskTitles.length; i++) {
            insertTaskIfMissing(con, projectIds[i], taskTitles[i], priorities[i],
                    taskStatuses[i], dueDates[i]);
        }

        String[][] snippets = {
                {"Retryable HTTP client", "Java", "http,retry,backend",
                        "public boolean shouldRetry(int status) { return status == 429 || status >= 500; }"},
                {"Debounced search hook", "TypeScript", "react,search,frontend",
                        "const useDebounced = (value, delay) => { const [v, setV] = useState(value); return v; };"},
                {"Useful Docker commands", "Shell", "docker,devops",
                        "docker compose up -d\\ndocker compose logs -f api\\ndocker compose down"},
                {"Binary search template", "Java", "algorithms,binary-search",
                        "int left = 0, right = values.length - 1; while (left <= right) { int mid = left + (right - left) / 2; }"},
                {"FastAPI health endpoint", "Python", "python,api,health",
                        "@app.get('/health')\\ndef health():\\n    return {'status': 'ok'}"},
                {"SQL pagination query", "SQL", "sql,pagination,mysql",
                        "SELECT * FROM tasks WHERE project_id=? ORDER BY due_date LIMIT ? OFFSET ?;"},
                {"Accessible button checklist", "Markdown", "a11y,frontend,checklist",
                        "- Visible focus state\\n- Descriptive label\\n- Keyboard accessible\\n- Disabled state"}
        };
        for (String[] snippet : snippets) {
            insertSnippetIfMissing(con, userId, snippet);
        }

        String[][] problems = {
                {"Two Sum", "EASY", "LeetCode", "https://leetcode.com/problems/two-sum/", "true", "Arrays"},
                {"Longest Substring Without Repeating Characters", "MEDIUM", "LeetCode", "https://leetcode.com/problems/longest-substring-without-repeating-characters/", "true", "Strings"},
                {"Merge Two Sorted Lists", "EASY", "LeetCode", "https://leetcode.com/problems/merge-two-sorted-lists/", "true", "Linked List"},
                {"Daily Temperatures", "MEDIUM", "LeetCode", "https://leetcode.com/problems/daily-temperatures/", "false", "Stack"},
                {"Binary Tree Level Order Traversal", "MEDIUM", "LeetCode", "https://leetcode.com/problems/binary-tree-level-order-traversal/", "true", "Trees"},
                {"Number of Islands", "MEDIUM", "LeetCode", "https://leetcode.com/problems/number-of-islands/", "false", "Graphs"},
                {"Coin Change", "MEDIUM", "LeetCode", "https://leetcode.com/problems/coin-change/", "false", "Dynamic Programming"}
        };
        for (String[] problem : problems) {
            insertProblemIfMissing(con, userId, problem);
        }

        String[][] bookmarks = {
                {"Spring Boot Reference", "https://docs.spring.io/spring-boot/docs/current/reference/html/", "Documentation", "java,spring,backend", "Official Spring Boot configuration and features reference."},
                {"MDN Fetch API", "https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API", "Web Development", "javascript,web,api", "Reliable browser reference for making network requests."},
                {"System Design Primer", "https://github.com/donnemartin/system-design-primer", "Interview Prep", "system-design,architecture", "A practical collection of system design interview notes."},
                {"Refactoring Guru Patterns", "https://refactoring.guru/design-patterns", "Learning", "patterns,oop,design", "Clear examples of common object-oriented design patterns."},
                {"MySQL 8.0 Manual", "https://dev.mysql.com/doc/refman/8.0/en/", "Database", "mysql,sql,database", "Reference for SQL syntax, indexing, and administration."},
                {"Git Flight Rules", "https://github.com/k88hudson/git-flight-rules", "Tools", "git,workflow", "Common Git recovery and workflow recipes."},
                {"Web Accessibility Initiative", "https://www.w3.org/WAI/fundamentals/accessibility-intro/", "Accessibility", "a11y,ux,web", "Introduction to accessible and inclusive web development."}
        };
        for (String[] bookmark : bookmarks) {
            insertBookmarkIfMissing(con, userId, bookmark);
        }

        for (int i = 0; i < projectIds.length; i++) {
            insertCodingSessionIfMissing(con, userId, projectIds[i],
                    "2026-10-" + String.format("%02d", 10 + i),
                    2700 + i * 600);
        }
    }

    private static int findUserId(Connection con, String username) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT user_id FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int findOrCreateProject(Connection con, int userId, String name,
                                           String description, String technologies,
                                           String status, String githubUrl) throws SQLException {
        try (PreparedStatement find = con.prepareStatement(
                "SELECT project_id FROM projects WHERE user_id=? AND name=?")) {
            find.setInt(1, userId);
            find.setString(2, name);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (PreparedStatement insert = con.prepareStatement(
                "INSERT INTO projects(user_id,name,description,technologies,status,github_url) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insert.setInt(1, userId);
            insert.setString(2, name);
            insert.setString(3, description);
            insert.setString(4, technologies);
            insert.setString(5, status);
            insert.setString(6, githubUrl);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not create demo project: " + name);
    }

    private static void insertTaskIfMissing(Connection con, int projectId, String title,
                                            String priority, String status, String dueDate)
            throws SQLException {
        if (exists(con, "SELECT task_id FROM tasks WHERE project_id=? AND title=?", projectId, title)) return;
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tasks(project_id,title,priority,status,due_date) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, projectId);
            ps.setString(2, title);
            ps.setString(3, priority);
            ps.setString(4, status);
            ps.setDate(5, Date.valueOf(dueDate));
            ps.executeUpdate();
        }
    }

    private static void insertSnippetIfMissing(Connection con, int userId, String[] snippet)
            throws SQLException {
        if (exists(con, "SELECT snippet_id FROM snippets WHERE user_id=? AND title=?", userId, snippet[0])) return;
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO snippets(user_id,title,language,tags,code) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, userId);
            ps.setString(2, snippet[0]);
            ps.setString(3, snippet[1]);
            ps.setString(4, snippet[2]);
            ps.setString(5, snippet[3]);
            ps.executeUpdate();
        }
    }

    private static void insertProblemIfMissing(Connection con, int userId, String[] problem)
            throws SQLException {
        try (PreparedStatement findTopic = con.prepareStatement(
                "SELECT topic_id FROM dsa_topics WHERE user_id=? AND name=?")) {
            findTopic.setInt(1, userId);
            findTopic.setString(2, problem[5]);
            try (ResultSet topic = findTopic.executeQuery()) {
                if (!topic.next()) return;
                int topicId = topic.getInt(1);
                if (exists(con, "SELECT problem_id FROM dsa_problems WHERE topic_id=? AND title=?",
                        topicId, problem[0])) return;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO dsa_problems(topic_id,title,difficulty,platform,url,solved) VALUES(?,?,?,?,?,?)")) {
                    ps.setInt(1, topicId);
                    ps.setString(2, problem[0]);
                    ps.setString(3, problem[1]);
                    ps.setString(4, problem[2]);
                    ps.setString(5, problem[3]);
                    ps.setBoolean(6, Boolean.parseBoolean(problem[4]));
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void insertBookmarkIfMissing(Connection con, int userId, String[] bookmark)
            throws SQLException {
        if (exists(con, "SELECT bookmark_id FROM bookmarks WHERE user_id=? AND title=?", userId, bookmark[0])) return;
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO bookmarks(user_id,title,url,category,tags,description) VALUES(?,?,?,?,?,?)")) {
            ps.setInt(1, userId);
            ps.setString(2, bookmark[0]);
            ps.setString(3, bookmark[1]);
            ps.setString(4, bookmark[2]);
            ps.setString(5, bookmark[3]);
            ps.setString(6, bookmark[4]);
            ps.executeUpdate();
        }
    }

    private static void insertCodingSessionIfMissing(Connection con, int userId, int projectId,
                                                     String date, int durationSeconds)
            throws SQLException {
        if (exists(con, "SELECT session_id FROM coding_sessions WHERE user_id=? AND project_id=? AND start_time LIKE ?",
                userId, projectId, date + "%")) return;
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO coding_sessions(user_id,project_id,start_time,end_time,duration_seconds) VALUES(?,?,?,?,?)")) {
            Timestamp start = Timestamp.valueOf(date + " 18:00:00");
            ps.setInt(1, userId);
            ps.setInt(2, projectId);
            ps.setTimestamp(3, start);
            ps.setTimestamp(4, Timestamp.valueOf(start.toLocalDateTime().plusSeconds(durationSeconds)));
            ps.setInt(5, durationSeconds);
            ps.executeUpdate();
        }
    }

    private static boolean exists(Connection con, String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
