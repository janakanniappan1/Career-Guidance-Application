-- ==============================================================================
-- MIGRATION: SEED 84 CANONICAL PRACTICE PROBLEMS (METADATA ONLY)
-- Ensures all 84 canonical problem IDs from DefaultPracticeCatalog.kt exist
-- in public.practice_problems to satisfy foreign key constraints from
-- public.user_problem_progress.
-- Preserves existing p1-p10 legacy rows.
-- Does NOT add interactive options/correct_index columns.
-- ==============================================================================

-- 1. Ensure public.practice_problems table and columns exist with proper defaults
CREATE TABLE IF NOT EXISTS public.practice_problems (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Coding',
    difficulty TEXT NOT NULL DEFAULT 'Medium',
    description TEXT DEFAULT '',
    career_id TEXT,
    topic TEXT,
    xp_reward INTEGER DEFAULT 20,
    acceptance_rate INTEGER DEFAULT 70,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'Coding';
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS difficulty TEXT DEFAULT 'Medium';
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS career_id TEXT;
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS topic TEXT;
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS xp_reward INTEGER DEFAULT 20;
ALTER TABLE public.practice_problems ADD COLUMN IF NOT EXISTS acceptance_rate INTEGER DEFAULT 70;

-- 2. Ensure RLS and permissions
ALTER TABLE public.practice_problems ENABLE ROW LEVEL SECURITY;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'practice_problems' AND policyname = 'Public read access for practice_problems'
    ) THEN
        CREATE POLICY "Public read access for practice_problems" ON public.practice_problems FOR SELECT USING (true);
    END IF;
END $$;
GRANT SELECT ON public.practice_problems TO anon, authenticated;

-- 3. Insert or Update all 52 Canonical Practice Problems
INSERT INTO public.practice_problems (
    id, title, category, difficulty, description, career_id, topic, xp_reward, acceptance_rate
) VALUES
('dsa_01', 'Two Sum & Hash Map Lookup', 'Coding', 'Easy', 'You are given an integer array nums and an integer target. Your task is to find the indices of the two distinct elements whose values add up to target. You may assume that each input has exactly one valid pair of numbers, and you cannot use the same element twice.

For nums = [2, 7, 11, 15] and target = 9, which algorithmic technique correctly solves the problem in optimal O(N) time complexity and O(N) auxiliary space?

Select the correct answer from the four options below.', 'software_dev', 'Arrays & Hashing', 20, 85),
('dsa_02', 'Reverse a Singly Linked List', 'Coding', 'Easy', 'You are given the head of a singly linked list containing sequential values 1 -> 2 -> 3 -> 4 -> 5 -> NULL. You need to reverse the direction of all pointer links in place so that the list becomes 5 -> 4 -> 3 -> 2 -> 1 -> NULL and return the new head node.

Which pointer manipulation technique achieves this in-place reversal in O(N) linear time using strictly O(1) auxiliary memory?

Select the correct answer from the four options below.', 'software_dev', 'Linked Lists', 20, 85),
('dsa_03', 'Longest Substring Without Repeating Characters', 'Coding', 'Medium', 'Given a string s = "abcabcbb", you need to find the length of the longest contiguous substring that contains no duplicate characters. For example, in "abcabcbb", the longest non-repeating substrings are "abc", "bca", and "cab", each with a length of 3.

Which algorithmic strategy finds the maximum length of a non-repeating substring in optimal O(N) time complexity?

Select the correct answer from the four options below.', 'software_dev', 'Sliding Window', 20, 68),
('dsa_04', 'Binary Tree Level Order Traversal', 'Coding', 'Medium', 'You are given the root of a binary tree: [3, 9, 20, null, null, 15, 7]. You are required to traverse the tree level by level from top to bottom, grouping node values horizontally from left to right for each depth level, producing [[3], [9, 20], [15, 7]].

Which data structure and traversal pattern guarantees that all nodes at depth d are processed before any nodes at depth d+1?

Select the correct answer from the four options below.', 'software_dev', 'Trees & Graphs', 20, 68),
('dsa_05', 'Climbing Stairs (Dynamic Programming)', 'Technical', 'Easy', 'You are climbing a staircase that has n steps to reach the top. Each time you take a step, you can climb either 1 step or 2 steps. Given n = 3 steps, there are 3 distinct ways to reach the top: (1+1+1), (1+2), and (2+1).

Which dynamic programming recurrence relation and base cases correctly compute the total number of distinct ways to climb n stairs in O(n) time and O(1) space?

Select the correct answer from the four options below.', 'software_dev', 'Dynamic Programming', 20, 85),
('aiml_01', 'Vector Dot Product & Cosine Similarity', 'Technical', 'Easy', 'A semantic search engine encodes document and query text into dense embedding vectors A and B of dimension D = 1536. To rank document relevance independent of vector magnitudes, the system computes the cosine of the angle between vectors A and B.

Which mathematical formula represents the Cosine Similarity between vector A and vector B?

Select the correct answer from the four options below.', 'aiml_engineer', 'Linear Algebra & Embeddings', 20, 85),
('aiml_02', 'Cross-Entropy Loss & Softmax Computation', 'Technical', 'Medium', 'A neural network classifier outputs uncalibrated real-valued logits z = [2.0, 1.0, 0.1] for C = 3 mutually exclusive classes. The model needs to convert these logits into a valid probability distribution p and compute the loss for a ground-truth one-hot label y where class 0 is the true target (y = [1, 0, 0]).

Which combination of activation function and loss function is the industry standard for multi-class classification, and why?

Select the correct answer from the four options below.', 'aiml_engineer', 'Deep Learning Fundamentals', 20, 68),
('aiml_03', 'K-Means Clustering Convergence', 'Technical', 'Medium', 'You are implementing the standard Lloyd''s algorithm for K-Means clustering on an unlabeled dataset of N feature vectors with K target cluster centroids. In each iteration, points are assigned to the nearest centroid and centroids are updated.

Which mathematical objective function does K-Means iteratively minimize to guarantee convergence to a local minimum?

Select the correct answer from the four options below.', 'aiml_engineer', 'Unsupervised Learning', 20, 68),
('aiml_04', 'Convolution Output Dimensions & Weight Sizing', 'Technical', 'Easy', 'A 2D convolutional layer receives an input image volume of dimensions 32 x 32 x 3 (Height x Width x Channels). The layer applies 16 filters, each of size 3 x 3, with a stride of S = 1 and zero padding of P = 0.

What are the spatial height, spatial width, and channel depth dimensions (H_out x W_out x C_out) of the resulting output feature map?

Select the correct answer from the four options below.', 'aiml_engineer', 'Computer Vision & CNNs', 20, 85),
('aiml_05', 'Precision-Recall Curve & Threshold Optimization', 'Technical', 'Medium', 'A machine learning model is deployed in a hospital to detect a rare malignant disease present in only 0.5% of tested patients. Failing to detect a true positive patient (a False Negative) has severe health consequences, while a False Positive results in a harmless follow-up screening.

Which evaluation metric must the engineering team prioritize and maximize when tuning the classification decision threshold?

Select the correct answer from the four options below.', 'aiml_engineer', 'Model Evaluation', 20, 68),
('data_03', 'Hypothesis Testing & A/B Test Power Calculation', 'Technical', 'Hard', 'A data science team is designing an online A/B experiment to evaluate a new checkout workflow. The baseline conversion rate is p = 5.0%, and the team wants to detect a minimum detectable effect (MDE) of +1.0% (to 6.0%) with a significance level of alpha = 0.05 (Type I error) and a statistical power of 1 - beta = 0.80.

In statistical hypothesis testing, what does the statistical power (1 - beta = 0.80) specifically represent?

Select the correct answer from the four options below.', 'data_scientist', 'Inferential Statistics', 20, 45),
('ds_01', 'Linear Regression: Normal Equation vs Gradient Descent', 'Technical', 'Medium', 'You need to train a multiple linear regression model on a dataset containing N = 500,000 training examples and D = 25,000 continuous features. You must choose between the analytical closed-form Normal Equation (theta = (X^T X)^(-1) X^T y) and iterative Batch Gradient Descent.

Why is iterative Gradient Descent strongly preferred over the analytical Normal Equation for this high-dimensional dataset?

Select the correct answer from the four options below.', 'data_scientist', 'Statistical Modeling', 20, 68),
('ds_02', 'Principal Component Analysis (PCA) & Variance Ratio', 'Technical', 'Medium', 'You have a dataset with 50 correlated numerical features. To reduce dimensionality while retaining at least 90% of the total dataset variance, you perform Principal Component Analysis (PCA) by computing the eigendecomposition of the sample covariance matrix Sigma = (1/N) * X_centered^T X_centered.

How does PCA construct these new orthogonal principal components and determine their individual importance?

Select the correct answer from the four options below.', 'data_scientist', 'Dimensionality Reduction', 20, 68),
('ds_03', 'Feature Scaling: Standardization vs Min-Max Normalization', 'Technical', 'Easy', 'You are preparing a machine learning dataset containing numerical features with varying units and scales. You partition the data into an 80% training set and a 20% test set. You want to apply StandardScaler (z = (x - mu) / sigma) to ensure gradient-based models converge smoothly without introducing data leakage.

Which workflow correctly applies feature scaling without leaking information from the test set into the model training pipeline?

Select the correct answer from the four options below.', 'data_scientist', 'Feature Engineering', 20, 85),
('ds_04', 'Random Forest Out-of-Bag (OOB) Error & Feature Importance', 'Technical', 'Medium', 'When training a Random Forest classifier with bootstrap aggregating (bagging) on N training samples, each individual decision tree is trained on a bootstrap sample drawn uniformly with replacement from the N instances.

Approximately what percentage of the original training instances are omitted from a given tree''s training set (known as Out-of-Bag samples), and how are they used?

Select the correct answer from the four options below.', 'data_scientist', 'Ensemble Methods', 20, 68),
('data_01', 'SQL Window Functions: Top N Sales per Region', 'Technical', 'Medium', 'You are given a sales database table sales_records with columns (rep_id, rep_name, region, revenue). Your manager asks for a report showing the top 3 highest-earning sales representatives in each geographic region. If two representatives in the same region have identical revenue, both should receive the same rank without skipping subsequent rank numbers.

Which SQL window function clause correctly calculates this regional rank?

Select the correct answer from the four options below.', 'data_analyst', 'SQL & Query Optimization', 20, 68),
('data_02', 'Missing Value Imputation & Outlier Detection', 'Technical', 'Easy', 'You are analyzing customer annual income data from a retail survey. The income distribution is heavily right-skewed with extreme positive outliers representing billionaire customers, as well as several missing values.

Which central tendency metric and outlier detection method are most robust and appropriate for this right-skewed distribution?

Select the correct answer from the four options below.', 'data_analyst', 'Data Wrangling & Statistics', 20, 85),
('da_01', 'SQL Rolling Averages & Cumulative Metric Tracking', 'Technical', 'Medium', 'An executive dashboard tracks daily online orders from a daily_sales table with columns (sale_date, daily_revenue). To smooth out weekend fluctuations, the leadership team requests a 7-day rolling average of daily revenue calculated for each day (the average of the current day plus the preceding 6 days).

Which SQL window frame specification correctly calculates this 7-day moving average?

Select the correct answer from the four options below.', 'data_analyst', 'Advanced SQL', 20, 68),
('da_02', 'Customer Cohort Retention & Churn Analysis', 'Technical', 'Easy', 'A SaaS subscription company began the month of January with exactly 2,000 active paying subscribers. During January, 100 existing subscribers cancelled their accounts, and the marketing team acquired 300 brand new subscribers, ending January with 2,200 active subscribers.

What is the customer churn rate for the month of January, and which formula defines it?

Select the correct answer from the four options below.', 'data_analyst', 'Business Analytics', 20, 85),
('da_03', 'Exploratory Data Analysis & Visual Distribution Skew', 'Technical', 'Easy', 'You generate a histogram and summary statistics for a company''s customer order value data. You notice that the histogram displays a long tail extending far to the right, and the statistical summary reveals Mean = $145, Median = $62, and Mode = $40.

How is this distribution classified, and what does the relationship between Mean, Median, and Mode signify?

Select the correct answer from the four options below.', 'data_analyst', 'Data Visualization', 20, 85),
('uiux_01', 'WCAG 2.1 AA Contrast Ratio & Touch Sizing', 'Technical', 'Easy', 'You are designing the design system for a mobile application that must comply with the Web Content Accessibility Guidelines (WCAG 2.1 Level AA) and Google Material Design touch ergonomics.

What is the minimum required visual color contrast ratio for standard body text (< 18pt regular font), and what is the minimum recommended touch target dimension for interactive components?

Select the correct answer from the four options below.', 'uiux_designer', 'Accessibility & Inclusive Design', 20, 85),
('uiux_02', 'Information Architecture & Card Sorting Analysis', 'Technical', 'Medium', 'A design team is reorganizing the navigation structure of an enterprise healthcare portal with over 60 distinct features. The team needs to understand how real users naturally group these healthcare services into mental categories before establishing primary menu categories.

Which research methodology should the team execute, and how do Open versus Closed card sorting differ?

Select the correct answer from the four options below.', 'uiux_designer', 'Information Architecture', 20, 68),
('uiux_03', 'Usability Testing & System Usability Scale (SUS)', 'Technical', 'Easy', 'Following a comprehensive usability test of a redesigned mobile banking app, 25 participants completed the standardized 10-item System Usability Scale (SUS) questionnaire with 5-point Likert scales.

What is the industry-standard benchmark SUS score that represents average (50th percentile) usability across software products?

Select the correct answer from the four options below.', 'uiux_designer', 'UX Research', 20, 85),
('uiux_04', 'Design Systems & Responsive Modular Grid Scaling', 'Technical', 'Medium', 'When building a scalable UI design system for cross-platform Android and web applications, design systems engineers standardize spacing, padding, margins, and component dimensions using a base 8dp/8px spatial grid.

Why is the 8dp baseline grid the universal standard in modern UI design systems rather than arbitrary pixel values (such as 7px or 13px)?

Select the correct answer from the four options below.', 'uiux_designer', 'Design Systems', 20, 68),
('uiux_05', 'Cognitive Load Reduction & Interaction Micro-States', 'Technical', 'Medium', 'An e-commerce checkout page presents users with 25 distinct form fields and decision options on a single overwhelming screen, resulting in high shopping cart abandonment. The design team refactors the checkout into a clear, 3-step progressive wizard with chunked inputs.

Which fundamental UX psychological law directly explains why reducing the number of immediate choices decreases user decision time and reduces cognitive friction?

Select the correct answer from the four options below.', 'uiux_designer', 'Interaction Design', 20, 68),
('cyber_01', 'OWASP Top 10: SQL Injection & XSS Defense', 'Technical', 'Medium', 'During a security audit of a web application, you find the following backend code: query = "SELECT * FROM accounts WHERE user_id = ''" + request.getParameter("id") + "''". An attacker inputs id = '' OR ''1''=''1'' -- to bypass authentication and dump the entire database.

Which primary defense mechanism completely neutralizes SQL Injection vulnerabilities at the architecture level?

Select the correct answer from the four options below.', 'cyber_analyst', 'Application Security', 20, 68),
('cyber_02', 'Public Key Infrastructure (PKI) & TLS Handshake', 'Technical', 'Medium', 'When a web browser establishes a secure HTTPS session with a bank web server using TLS 1.3, asymmetric cryptography and symmetric cryptography are both utilized.

Why does TLS use asymmetric cryptography during the initial handshake, but switch to symmetric encryption (such as AES-GCM) for the actual session data transmission?

Select the correct answer from the four options below.', 'cyber_analyst', 'Cryptography & Network Security', 20, 68),
('cyber_03', 'Network Traffic Forensics & Packet Inspection', 'Technical', 'Medium', 'A Security Operations Center (SOC) analyst inspects network firewall logs and observes an external IP address sending 200,000 TCP SYN packets per second to port 443 with spoofed source IPs, but never returning any corresponding ACK packets to complete the TCP three-way handshake.

What type of cyber attack is occurring, and what server resource does it exhaust?

Select the correct answer from the four options below.', 'cyber_analyst', 'Network Forensics', 20, 68),
('cyber_04', 'Role-Based Access Control (RBAC) & OAuth 2.0 Flow', 'Technical', 'Easy', 'A mobile banking app needs to allow users to authorize access to their financial transactions on a third-party budgeting service. Because mobile applications are public clients that cannot securely store a private client_secret, standard Authorization Code Flow is vulnerable to code interception.

Which OAuth 2.0 extension protocol is mandatory for securing public mobile and Single-Page applications?

Select the correct answer from the four options below.', 'cyber_analyst', 'Identity & Access Management', 20, 85),
('cyber_05', 'Incident Response Playbook & SIEM IoC Correlation', 'Technical', 'Hard', 'A Security Information and Event Management (SIEM) system detects confirmed ransomware executing on three enterprise workstations and attempting lateral movement across the internal subnet. The SOC incident response team must immediately follow the standard NIST SP 800-61 incident handling lifecycle.

What is the correct sequential order of the NIST Incident Response lifecycle phases?

Select the correct answer from the four options below.', 'cyber_analyst', 'Security Operations', 20, 45),
('cloud_01', 'Docker Containerization & Multi-Stage Builds', 'Technical', 'Easy', 'You are creating a Dockerfile for a production Java Spring Boot microservice. Compiling the application requires Maven (500 MB) and a full JDK (400 MB), but running the compiled JAR file only requires a minimal Java Runtime Environment (JRE Alpine, 80 MB).

Which Dockerfile design pattern allows you to build the application with full build tools while keeping the final production container image minimal and secure?

Select the correct answer from the four options below.', 'cloud_engineer', 'Containers & CI/CD', 20, 85),
('cloud_02', 'Kubernetes Pod Auto-Scaling & Load Balancing', 'Technical', 'Medium', 'A microservice deployed on a Kubernetes production cluster experiences sudden traffic surges during flash sales, causing pod CPU utilization to spike from 30% to 90%. The infrastructure team needs the deployment to automatically scale out the number of running pod replicas when average CPU exceeds 70%.

Which Kubernetes native resource is designed to monitor metrics and automatically adjust the replica count of a Deployment?

Select the correct answer from the four options below.', 'cloud_engineer', 'Cloud Orchestration', 20, 68),
('cloud_03', 'Linux Administration & Graceful Process Signals', 'Technical', 'Easy', 'When deploying a new version of a web application running as a Linux systemd service or Docker container, the orchestration orchestrator must instruct the active application process to stop accepting new requests, finish processing in-flight database transactions, and terminate cleanly.

Which POSIX signal should be sent first to initiate a graceful application shutdown, and which signal forcefully kills the process if it fails to exit?

Select the correct answer from the four options below.', 'cloud_engineer', 'Linux Fundamentals', 20, 85),
('cloud_04', 'VPC Subnetting & High-Availability Network Design', 'Technical', 'Medium', 'You are designing the network architecture for a 3-tier enterprise cloud application in AWS or GCP. The production database cluster contains sensitive financial customer records. The database servers must be able to initiate outbound connections to the Internet to download operating system security updates, but they must NEVER be accessible from the public Internet.

Which Virtual Private Cloud (VPC) subnet and routing configuration fulfills these security requirements?

Select the correct answer from the four options below.', 'cloud_engineer', 'Cloud Networking', 20, 68),
('cloud_05', 'CI/CD Pipeline Automation & Blue-Green Traffic Shifting', 'Technical', 'Medium', 'An enterprise SaaS platform cannot tolerate any downtime during production releases. The engineering team implements a Blue-Green Deployment strategy to deploy version 2.0 while version 1.0 is currently handling live production traffic.

How does a Blue-Green deployment achieve zero-downtime releases and rapid rollback capability?

Select the correct answer from the four options below.', 'cloud_engineer', 'DevOps & CI/CD', 20, 68),
('apt_01', 'Time and Work: Alternate Work Days', 'Aptitude', 'Medium', 'A can complete a piece of work in 10 days, while B can complete the same work in 15 days. They work on alternate days, with A working on Day 1 and B working on Day 2. They continue this alternating pattern (A on odd days, B on even days) until the entire work is completed.

How many total days will they take to complete the entire work?

Select the correct answer from the four options below.', NULL, 'Time and Work', 20, 68),
('apt_02', 'Probability: Two Cards of the Same Suit', 'Aptitude', 'Easy', 'Two cards are drawn randomly one after another without replacement from a standard, well-shuffled pack of 52 playing cards (containing 4 suits with 13 cards in each suit).

What is the exact probability that both drawn cards belong to the same suit?

Select the correct answer from the four options below.', NULL, 'Probability', 20, 85),
('apt_03', 'Problems on Trains: Crossing in Opposite Directions', 'Aptitude', 'Medium', 'Two trains with lengths of 150 meters and 200 meters are running on parallel railway tracks in opposite directions toward each other. Train 1 travels at a uniform speed of 54 km/h and Train 2 travels at a uniform speed of 72 km/h.

How many seconds will they take to completely cross each other from the moment their front engines meet?

Select the correct answer from the four options below.', NULL, 'Time, Speed and Distance', 20, 68),
('apt_04', 'Profit and Loss: Successive Discounts', 'Aptitude', 'Easy', 'A store manager marks a designer jacket at a Marked Price (MP) of $500. During a festive sale, the store offers two successive discounts of 20% and 10% on the marked price.

What is the single equivalent discount percentage, and what is the final selling price paid by a customer?

Select the correct answer from the four options below.', NULL, 'Profit and Loss', 20, 85),
('apt_05', 'Problems on Ages: Father and Son', 'Aptitude', 'Medium', 'The ratio of the present ages of a father and his son is 5 : 2. Exactly 5 years from now, the sum of their ages will be 80 years.

What is the present age of the father?

Select the correct answer from the four options below.', NULL, 'Ages', 20, 68),
('apt_06', 'Pipes and Cisterns: Two Inflow Pipes', 'Aptitude', 'Easy', 'Pipe A can fill an empty water reservoir in 6 hours, while Pipe B can fill the same empty reservoir in 8 hours. If both pipes are opened simultaneously into the empty reservoir, how many hours will they take to fill the reservoir completely?

Select the correct answer from the four options below.', NULL, 'Pipes and Cisterns', 20, 85),
('apt_07', 'Ratio and Proportion: Mixture Dilution', 'Aptitude', 'Medium', 'A container holds 60 liters of a milk and water mixture where the ratio of milk to water is 2 : 1. A dairy worker wants to dilute the mixture by adding pure water so that the new ratio of milk to water becomes 1 : 2.

How many liters of pure water must be added to the mixture?

Select the correct answer from the four options below.', NULL, 'Ratio and Proportion', 20, 68),
('apt_08', 'Simple Interest: Multiplication of Principal', 'Aptitude', 'Easy', 'A sum of money invested under Simple Interest at a constant annual interest rate doubles itself in exactly 8 years.

In how many total years will the principal amount become 4 times its original value at the exact same rate of simple interest?

Select the correct answer from the four options below.', NULL, 'Simple and Compound Interest', 20, 85),
('apt_09', 'Averages: Team Batting Average Change', 'Aptitude', 'Easy', 'In a cricket match, the average score of the first 5 batsmen was 50 runs. When the 6th batsman batted and his score was included, the overall average score of all 6 batsmen increased to 55 runs.

How many runs did the 6th batsman score in the match?

Select the correct answer from the four options below.', NULL, 'Average', 20, 85),
('apt_10', 'Percentages: Price Increase & Consumption Adjustment', 'Aptitude', 'Easy', 'Due to global market inflation, the retail price of petrol increases by 25%. A vehicle owner wants to adjust their monthly driving habits so that their total financial expenditure on petrol remains exactly unchanged.

By what percentage must the owner reduce their petrol consumption to keep expenditure constant?

Select the correct answer from the four options below.', NULL, 'Percentages', 20, 85),
('iv_01', 'STAR Method: Resolving Technical Disagreements', 'Interview', 'Easy', 'During a behavioral interview for a software engineering position, the interviewer asks: ''Tell me about a time you had a strong technical disagreement with a teammate on an architectural decision. How did you handle it?''

Which structured communication strategy and behavioral approach demonstrates the highest level of professional maturity and engineering leadership?

Select the correct answer from the four options below.', NULL, 'Behavioral Interviews', 20, 85),
('iv_02', 'System Design: Mitigating Read-Heavy Database Bottlenecks', 'Interview', 'Medium', 'In a system design interview, you are asked to scale an e-commerce platform. During flash sale events, the platform experiences severe database read latency spikes where 92% of incoming queries are repeated reads of product catalog data, pushing the primary relational database CPU to 95%.

Which architectural strategy is most appropriate and cost-effective as the initial mitigation to relieve read database pressure?

Select the correct answer from the four options below.', NULL, 'System Design', 20, 68),
('iv_03', 'Engineering Leadership: Communicating Technical Debt', 'Interview', 'Easy', 'During a sprint planning meeting, your engineering team identifies that legacy database query code is causing intermittent checkout timeouts and slowing down feature development. Non-technical product managers want to prioritize new customer-facing features instead of refactoring.

How should an engineer effectively communicate and justify allocating sprint capacity toward technical debt reduction to business stakeholders?

Select the correct answer from the four options below.', NULL, 'Stakeholder Management', 20, 85),
('iv_04', 'Site Reliability: Blameless Post-Mortem Principles', 'Interview', 'Medium', 'A junior engineer accidentally deploys an incorrect environment configuration change that brings down the production billing service for 45 minutes before being rolled back. In a high-performing Site Reliability Engineering (SRE) culture, the team conducts a blameless post-mortem.

What is the primary objective of conducting a blameless post-mortem following a production incident?

Select the correct answer from the four options below.', NULL, 'Production Operations', 20, 68),
('tech_01', 'Database Indexing: B+ Trees vs Hash Indexing', 'Technical', 'Medium', 'You are optimizing a relational database table orders containing 10 million rows. Applications frequently execute range queries such as: SELECT * FROM orders WHERE order_date BETWEEN ''2026-01-01'' AND ''2026-03-31''.

Why is a B+ Tree index structure significantly superior to a Hash Index for executing this range scan query?

Select the correct answer from the four options below.', NULL, 'DBMS', 20, 68),
('tech_02', 'REST vs gRPC & Protocol Buffers', 'Technical', 'Medium', 'An engineering team is designing high-throughput, low-latency inter-service Remote Procedure Call (RPC) communication between hundreds of backend microservices. They must choose between traditional REST over HTTP/1.1 with JSON payloads and gRPC over HTTP/2 with Protocol Buffers.

Which architectural advantages make gRPC with Protocol Buffers significantly faster and more network-efficient than REST with JSON for internal microservices?

Select the correct answer from the four options below.', NULL, 'Distributed Systems', 20, 68),
('tech_03', 'Process Synchronization & Mutex vs Semaphore', 'Technical', 'Medium', 'A multi-threaded server application manages access to a fixed pool of N = 5 database connections shared among 50 worker threads. Threads must acquire a connection before executing queries and release it back to the pool immediately upon completion.

What is the key functional difference between a Mutex (Mutual Exclusion lock) and a Counting Semaphore in managing concurrency and resource access?

Select the correct answer from the four options below.', NULL, 'Operating Systems', 20, 68),
('apt_11', 'Number Systems: Modular Remainder Theorem', 'Aptitude', 'Medium', 'When an integer N is divided by 13, the remainder is 7. If the square of the same integer (N^2) is divided by 13, what is the resulting remainder?

Select the correct remainder from the options below.', NULL, 'Number Systems', 20, 68),
('apt_12', 'Profit and Loss: Markup and Cash Discount', 'Aptitude', 'Medium', 'A electronics vendor marks their inventory 40% above the cost price (CP) and offers a 15% trade discount on the marked price for direct payments. What is the vendor''s net profit percentage on these sales?

Select the correct percentage from the options below.', NULL, 'Profit and Loss', 20, 68),
('apt_13', 'Time, Speed and Distance: Stream Current Speed', 'Aptitude', 'Medium', 'A boat navigates 36 km downstream along a river in 3 hours and returns upstream covering 24 km in 4 hours. What is the rate of flow (speed) of the river current?

Select the correct speed in km/h from the options below.', NULL, 'Time, Speed & Distance', 20, 68),
('apt_14', 'Time and Work: Equivalence and Combined Team Rate', 'Aptitude', 'Medium', 'A construction task can be completed in 14 days by either 12 men or 18 women. Working at the same steady rate, how many days will a combined workforce of 8 men and 16 women take to complete the exact same task?

Select the correct number of days from the options below.', NULL, 'Time and Work', 20, 68),
('apt_15', 'Ratios & Proportions: Partnership Profit Distribution', 'Aptitude', 'Medium', 'Three tech founders A, B, and C invest capital into a cloud venture. A invests ₹40,000 for 12 months, B invests ₹60,000 for 8 months, and C invests ₹80,000 for 6 months. If the annual profit generated is ₹1,05,000, what is B''s profit share?

Select the correct profit amount in Rupees from the options below.', NULL, 'Ratios & Proportions', 20, 68),
('apt_16', 'Averages: Weighted Average Across Batches', 'Aptitude', 'Medium', 'In an engineering college department, Section A has 40 students with an average score of 76 in a data structures exam, while Section B has 60 students with an average score of 84. What is the combined mean score across all 100 students?

Select the correct mean score from the options below.', NULL, 'Averages', 20, 68),
('apt_17', 'Percentages & Set Theory: Campus Placement Offer Distribution', 'Aptitude', 'Medium', 'In a graduating college batch, 65% of students received job offers in software engineering, 45% received offers in data analytics, and 20% received offers in both domains. What percentage of students received an offer in at least one of these two domains?

Select the correct percentage from the options below.', NULL, 'Percentages', 20, 68),
('apt_18', 'Probability: Independent Vulnerability Detection Scanners', 'Aptitude', 'Medium', 'Two independent security tools, Scanner A and Scanner B, inspect a microservice for vulnerabilities. Scanner A has a 0.80 probability of detecting a flaw, while Scanner B has a 0.75 probability. If a flaw exists, what is the probability that it is detected by at least one scanner?

Select the correct probability from the options below.', NULL, 'Probability', 20, 68),
('apt_19', 'Logical Reasoning: Directional Tracking and Displacement', 'Aptitude', 'Medium', 'An autonomous delivery drone leaves a dispatch hub and flies 12 km North. It then executes a 90-degree right turn and travels 9 km East. Finally, it turns right again and flies 12 km South to its delivery destination. What is the drone''s net displacement distance and direction from the hub?

Select the correct displacement from the options below.', NULL, 'Logical Reasoning', 20, 68),
('apt_20', 'Data Interpretation: Growth Rate of Cloud Infrastructure Cost', 'Aptitude', 'Medium', 'A technology startup reviews its quarterly cloud expenditure reports:
• Q1 Compute Cost: $60,000 (out of $120,000 total)
• Q2 Compute Cost: $90,000 (out of $150,000 total)
• Q3 Compute Cost: $108,000 (out of $180,000 total)

What is the percentage increase in Compute expenditure from Q1 to Q3?

Select the correct percentage increase from the options below.', NULL, 'Data Interpretation', 20, 68),
('coding_01', 'Two Pointers: Valid Palindrome String Validation', 'Coding', 'Easy', 'Consider the following algorithm for checking whether a string s is a valid palindrome while ignoring non-alphanumeric characters and case:

def is_palindrome(s: str) -> bool:
    left, right = 0, len(s) - 1
    while left < right:
        while left < right and not s[left].isalnum():
            left += 1
        while left < right and not s[right].isalnum():
            right -= 1
        if s[left].lower() != s[right].lower():
            return False
        left += 1
        right -= 1
    return True

What is the asymptotic time complexity and auxiliary space complexity of this implementation?', 'software_dev', 'Strings & Two Pointers', 20, 85),
('coding_02', 'Loops & Iteration: Enumerate Output Prediction', 'Coding', 'Medium', 'Predict the output of the following Python program that processes a list of integers using enumerate:

nums = [1, 2, 3, 4, 5]
result = []
for i, x in enumerate(nums):
    if x % 2 == 0:
        result.append(x * 2)
    else:
        result.append(x + i)
print(result)

What is printed when this snippet runs?', 'aiml_engineer', 'Loops & Conditionals', 20, 68),
('coding_03', 'Recursion: Call Stack and Base Case Execution', 'Coding', 'Easy', 'Examine the following recursive function definition:

def mystery(n: int) -> int:
    if n <= 1:
        return 1
    return n + mystery(n - 2)

What integer value is returned when calling mystery(7)?

Select the correct return value from the options below.', 'software_dev', 'Recursion', 20, 85),
('coding_04', 'Sorting Algorithms: Stability and Worst-Case Bounds', 'Coding', 'Medium', 'Which sorting algorithm guarantees a worst-case time complexity of O(N log N) while also maintaining sorting stability (preserving the relative original order of duplicate keys)?

Select the correct algorithm from the options below.', 'software_dev', 'Sorting Algorithms', 20, 68),
('coding_05', 'Arrays & Sliding Window: Maximum Sum Subarray of Size K', 'Coding', 'Medium', 'Given an integer array nums = [2, 1, 5, 1, 3, 2] and a window size k = 3, what is the maximum sum of any contiguous subarray of size k, and what is the optimal time complexity to find it?

Select the correct maximum sum and complexity from the options below.', 'data_scientist', 'Sliding Window', 20, 68),
('coding_06', 'Functions & Scope: Mutable Default Arguments Trap', 'Coding', 'Medium', 'Analyze the following Python code snippet containing a function with a default parameter:

def append_to_list(val, items=[]):
    items.append(val)
    return items

list1 = append_to_list(10)
list2 = append_to_list(20)
print(list1)

What is the exact output printed by print(list1)?', 'aiml_engineer', 'Functions & Scope', 20, 68),
('coding_07', 'OOP Principles: Information Hiding and Encapsulation', 'Coding', 'Easy', 'In Object-Oriented Programming (OOP), which core principle restricts direct external access to an object''s internal state variables and requires callers to interact through well-defined public methods and interfaces?

Select the correct OOP principle from the options below.', 'software_dev', 'Object-Oriented Programming', 20, 85),
('coding_08', 'Conditionals: Boolean Operator Short-Circuit Evaluation', 'Coding', 'Easy', 'Consider the following Python function:

def check_ratio(x: int, y: int) -> bool:
    return x > 0 and (y / x) > 2

What occurs when the function is called with arguments check_ratio(0, 5)?

Select the correct runtime behavior from the options below.', 'software_dev', 'Conditionals & Logic', 20, 85),
('coding_09', 'Searching Algorithms: Binary Search Applicability and Bound', 'Coding', 'Easy', 'A developer needs to search for an integer in an array containing N = 1,000,000 elements. Under what structural precondition is Binary Search applicable, and what is its maximum number of comparisons in the worst case?

Select the correct prerequisite and bound from the options below.', 'data_analyst', 'Searching Algorithms', 20, 85),
('coding_10', 'Complexity Analysis: Asymptotic Growth of Nested While Loops', 'Coding', 'Medium', 'Analyze the asymptotic time complexity of the following algorithm in Big-O notation:

def process_matrix(n: int) -> int:
    count = 0
    i = 1
    while i < n:
        j = 0
        while j < n:
            count += 1
            j += 1
        i *= 2
    return count

What is the tight worst-case time complexity of process_matrix(n)?', 'software_dev', 'Time Complexity Analysis', 20, 68),
('coding_11', 'Debugging: Array Slicing Boundary and Upper Index Off-by-One', 'Coding', 'Easy', 'A developer writes the following Python code intending to extract a sublist from index 1 through index 4 inclusive from arr:

arr = [10, 20, 30, 40, 50, 60]
sub = arr[1:4]
print(sub)

The program prints [20, 30, 40] instead of [20, 30, 40, 50]. Why did this happen, and what is the proper correction?

Select the correct root cause and fix from the options below.', 'software_dev', 'Debugging & Arrays', 20, 85),
('iv_05', 'Technical Interviews: Navigating Unfamiliar Technologies', 'Interview', 'Easy', 'During a technical interview for a software engineering role, the interviewer asks how you would implement a distributed consensus algorithm that you have never studied or used before. What is the most professionally effective way to respond?

Select the best approach from the options below.', 'software_dev', 'Interview Strategy', 20, 85),
('iv_06', 'Project Presentation: Articulating Architecture and Impact', 'Interview', 'Medium', 'When an interviewer asks: ''Walk me through the most significant machine learning project listed on your resume,'' how should you structure your explanation to make the strongest professional impression?

Select the most effective structure from the options below.', 'aiml_engineer', 'Project Presentation', 20, 68),
('iv_07', 'Teamwork: Constructive Response to Critical Code Review', 'Interview', 'Easy', 'You submit a pull request (PR) for a feature. A senior engineer leaves several critical comments suggesting that your implementation be refactored for better modularity and testability. What is the most constructive way to respond?

Select the best professional action from the options below.', 'software_dev', 'Workplace Communication', 20, 85),
('iv_08', 'Resume Best Practices: Honest Representation of Technical Skills', 'Interview', 'Easy', 'A 2nd-year computer science student completed a 1-day introductory tutorial on Docker. How should this student accurately and professionally represent their containerization skills on their resume for internship applications?

Select the most appropriate approach from the options below.', 'software_dev', 'Resume Best Practices', 20, 85),
('iv_09', 'Analytical Problem Solving: Deconstructing Vague Metrics', 'Interview', 'Medium', 'In an interview for a data analyst or engineering role, the interviewer presents a scenario: ''Our active user engagement dropped 15% over the past week. How would you investigate this?'' What is the most structured initial step?

Select the best analytical strategy from the options below.', 'data_analyst', 'Problem Solving', 20, 68),
('iv_10', 'HR Interviews: Answering Long-Term Career Goals', 'Interview', 'Easy', 'During an HR round for a campus placement role, the recruiter asks: ''Where do you see yourself professionally in 3 to 5 years?'' What is the most compelling and balanced response strategy?

Select the most effective answer strategy from the options below.', 'software_dev', 'HR Interview', 20, 85),
('iv_11', 'Engineering Ethics: Deadlines vs Software Quality & Safety', 'Interview', 'Medium', 'Your team faces an imminent deadline to deploy a critical production patch. Skipping automated regression tests would allow deployment 2 hours ahead of the deadline, but risks introducing runtime bugs. What is the standard professional engineering approach?

Select the most responsible engineering action from the options below.', 'cloud_engineer', 'Professional Engineering', 20, 68),
('iv_12', 'Placement Interviews: Structuring ''Tell Me About Yourself''', 'Interview', 'Easy', 'At the beginning of an on-campus placement interview, the interviewer asks: ''Tell me about yourself.'' Which narrative framework produces the most professional and memorable introduction?

Select the most effective structure from the options below.', 'aiml_engineer', 'HR Interview', 20, 85),
('iv_13', 'Internship Success: Proactive Onboarding and Ramp-Up', 'Interview', 'Easy', 'During the first week of your software engineering internship, you are provided with your development laptop and repository access. What proactive approach best accelerates your ramp-up and leaves a positive impression on your team?

Select the best onboarding strategy from the options below.', 'software_dev', 'Internship Success', 20, 85),
('iv_14', 'Behavioral Questions: Addressing Past Mistakes and Failures', 'Interview', 'Medium', 'In a behavioral interview round, the interviewer asks: ''Tell me about a time you made a significant mistake or experienced failure on a technical project.'' What makes a candidate''s response authentic and compelling?

Select the most effective response from the options below.', 'software_dev', 'Behavioral Interview', 20, 68),
('iv_15', 'Workplace Etiquette: Formal Offer Evaluation and Communication', 'Interview', 'Easy', 'You receive a job offer through your college''s campus placement drive. What is the most professional conduct when evaluating and communicating your decision regarding the offer?

Select the most appropriate conduct from the options below.', 'software_dev', 'Workplace Etiquette', 20, 85)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    category = EXCLUDED.category,
    difficulty = EXCLUDED.difficulty,
    description = EXCLUDED.description,
    career_id = EXCLUDED.career_id,
    topic = EXCLUDED.topic,
    xp_reward = EXCLUDED.xp_reward,
    acceptance_rate = EXCLUDED.acceptance_rate;

