CREATE DATABASE IF NOT EXISTS developer_workspace;
USE developer_workspace;

CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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
);

CREATE TABLE IF NOT EXISTS tasks (
    task_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS snippets (
    snippet_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    language VARCHAR(40) NOT NULL,
    tags VARCHAR(300),
    code TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dsa_topics (
    topic_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    name VARCHAR(80) NOT NULL,
    UNIQUE(user_id, name),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dsa_problems (
    problem_id INT PRIMARY KEY AUTO_INCREMENT,
    topic_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    platform VARCHAR(100),
    url VARCHAR(500),
    solved BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (topic_id) REFERENCES dsa_topics(topic_id) ON DELETE CASCADE
);

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
);

CREATE TABLE IF NOT EXISTS coding_sessions (
    session_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    project_id INT,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    duration_seconds INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE SET NULL
);

CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_snippets_user ON snippets(user_id);
CREATE INDEX idx_bookmarks_user ON bookmarks(user_id);
CREATE INDEX idx_sessions_user ON coding_sessions(user_id);
CREATE INDEX idx_dsa_topic ON dsa_problems(topic_id);
