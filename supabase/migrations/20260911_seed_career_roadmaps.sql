-- ==============================================================================
-- CGA ROADMAP DATABASE MIGRATION & SEED
-- Migration: 20260911_seed_career_roadmaps.sql
-- Source of Truth: DefaultRoadmapCatalog.kt
--
-- Schema Conformance:
--   public.roadmap_stages (id TEXT, roadmap_id UUID, stage_name TEXT, stage_order INTEGER, subtitle TEXT, created_at TIMESTAMPTZ)
--   public.roadmap_tasks (id TEXT, stage_id TEXT, title TEXT, description TEXT, skill_category TEXT, estimated_hours INTEGER, resource_type TEXT, task_order INTEGER, created_at TIMESTAMPTZ)
--
-- Critical Rules:
--   1. NO ALTER TABLE or added columns.
--   2. NO new rows in public.roadmaps (7 verified production UUIDs reused).
--   3. Exact original TEXT IDs from DefaultRoadmapCatalog.kt (no UUID conversion).
--   4. Exact career-specific content across 44 stages and 125 tasks.
--   5. user_task_progress is completely untouched.
--   6. NO DELETE, TRUNCATE, DROP, or CASCADE operations.
--   7. Idempotent via ON CONFLICT (id) DO UPDATE.
-- ==============================================================================

-- 1. Ensure Careers Exist (Foreign Key Prerequisite, ON CONFLICT DO NOTHING)
INSERT INTO public.careers (id, title, description, demand_level, avg_salary_lpa, job_roles, is_active)
VALUES
    ('aiml_engineer', 'AI / Machine Learning Engineer', 'Design, build, and deploy machine learning models and intelligent systems.', 'HIGH', 14.5, ARRAY['ML Engineer', 'AI Specialist', 'Research Scientist'], true),
    ('software_dev', 'Software Developer', 'Design, develop, test, and maintain robust software applications and scalable systems.', 'HIGH', 11.5, ARRAY['Full Stack Developer', 'Backend Engineer', 'Mobile App Developer'], true),
    ('data_analyst', 'Data Analyst', 'Transform raw data into meaningful business insights through statistical analysis and dashboards.', 'MEDIUM', 9.0, ARRAY['Data Analyst', 'BI Specialist', 'Analytics Consultant'], true),
    ('data_scientist', 'Data Scientist', 'Analyze complex datasets using machine learning, statistical modeling, and data engineering.', 'HIGH', 13.5, ARRAY['Data Scientist', 'Quantitative Analyst', 'Decision Scientist'], true),
    ('uiux_designer', 'UI / UX Designer', 'Craft intuitive, accessible, and engaging digital experiences and user interfaces.', 'MEDIUM', 10.0, ARRAY['UI Designer', 'UX Researcher', 'Product Designer'], true),
    ('cyber_analyst', 'Cybersecurity Analyst', 'Protect organizational networks, systems, and sensitive data from cyber threats and attacks.', 'HIGH', 12.0, ARRAY['Security Analyst', 'SOC Analyst', 'Penetration Tester'], true),
    ('cloud_engineer', 'Cloud Engineer', 'Architect, deploy, and manage scalable and secure cloud infrastructure and services.', 'HIGH', 13.0, ARRAY['Cloud Architect', 'DevOps Engineer', 'Site Reliability Engineer'], true)
ON CONFLICT (id) DO NOTHING;

-- 2. Upsert 44 Career-Specific Roadmap Stages (Exact Catalog TEXT IDs)
INSERT INTO public.roadmap_stages (id, roadmap_id, stage_name, stage_order, subtitle)
VALUES
    ('stage_1', '0c09e0ea-be2a-4c15-b39d-a005783af247', '1. Foundation', 1, 'Mathematical grounding, Git version control, and Linux basics'),
    ('stage_2', '0c09e0ea-be2a-4c15-b39d-a005783af247', '2. Programming', 2, 'Python mastery and algorithmic data structures'),
    ('stage_3', '0c09e0ea-be2a-4c15-b39d-a005783af247', '3. Core Skills', 3, 'Supervised/unsupervised algorithms and relational data'),
    ('stage_4', '0c09e0ea-be2a-4c15-b39d-a005783af247', '4. Advanced Skills', 4, 'Deep Learning, PyTorch, Transformers, and MLOps'),
    ('stage_5', '0c09e0ea-be2a-4c15-b39d-a005783af247', '5. Projects', 5, 'Portfolio building with end-to-end deployed AI systems'),
    ('stage_6', '0c09e0ea-be2a-4c15-b39d-a005783af247', '6. Placement Preparation', 6, 'Aptitude, DSA interview coding, resume review & mock rounds'),
    ('s1', '0e7313b9-7f2c-4bdb-88fc-5b48213b16fb', '1. Foundation', 1, 'CS basics, Git, and Operating Systems'),
    ('s2', '0e7313b9-7f2c-4bdb-88fc-5b48213b16fb', '2. Programming', 2, 'Modern Kotlin & Java Core'),
    ('s3', '0e7313b9-7f2c-4bdb-88fc-5b48213b16fb', '3. Core Skills', 3, 'APIs, Databases & Clean Architecture'),
    ('s4', '0e7313b9-7f2c-4bdb-88fc-5b48213b16fb', '4. Advanced Skills', 4, 'Microservices, Caching & Concurrency'),
    ('s5', '0e7313b9-7f2c-4bdb-88fc-5b48213b16fb', '5. Projects', 5, 'Full-Stack Production Application'),
    ('s6', '0e7313b9-7f2c-4bdb-88fc-5b48213b16fb', '6. Placement Preparation', 6, 'Coding contests, resume & mock interviews'),
    ('da1', '7a00e299-6858-46bf-9e85-60cab5ee17ac', '1. Foundation', 1, 'Statistics and Spreadsheets'),
    ('da2', '7a00e299-6858-46bf-9e85-60cab5ee17ac', '2. Programming', 2, 'Python for Data Analysis'),
    ('da3', '7a00e299-6858-46bf-9e85-60cab5ee17ac', '3. Core Skills', 3, 'SQL & Relational Databases'),
    ('da4', '7a00e299-6858-46bf-9e85-60cab5ee17ac', '4. Advanced Skills', 4, 'BI Dashboards & Storytelling'),
    ('da5', '7a00e299-6858-46bf-9e85-60cab5ee17ac', '5. Projects', 5, 'Real-world Business Analytics Case Studies'),
    ('da6', '7a00e299-6858-46bf-9e85-60cab5ee17ac', '6. Placement Preparation', 6, 'SQL interview tests & case interview prep'),
    ('ds_stage_1', '4a62809e-15f4-41b3-8e7a-f37f965578ad', '1. Foundations & Python', 1, 'Python programming fundamentals, Git version control, and problem solving'),
    ('ds_stage_2', '4a62809e-15f4-41b3-8e7a-f37f965578ad', '2. Mathematics & Statistics', 2, 'Probability theory, inferential statistics, linear algebra, and calculus'),
    ('ds_stage_3', '4a62809e-15f4-41b3-8e7a-f37f965578ad', '3. Data Handling & SQL', 3, 'NumPy arrays, Pandas wrangling, exploratory analysis, and SQL queries'),
    ('ds_stage_4', '4a62809e-15f4-41b3-8e7a-f37f965578ad', '4. Machine Learning Foundations', 4, 'Supervised and unsupervised algorithms, feature engineering, and validation'),
    ('ds_stage_5', '4a62809e-15f4-41b3-8e7a-f37f965578ad', '5. Advanced Data Science', 5, 'Ensemble learning, hyperparameter tuning, time series, and NLP'),
    ('ds_stage_6', '4a62809e-15f4-41b3-8e7a-f37f965578ad', '6. Real-World Projects', 6, 'End-to-end data science projects from raw data to business recommendations'),
    ('ds_stage_7', '4a62809e-15f4-41b3-8e7a-f37f965578ad', '7. Placement Preparation', 7, 'Technical coding, SQL assessments, ML conceptual rounds, and mock interviews'),
    ('ui_stage_1', 'dfa91e34-0bcb-4a2b-a047-25fcb9574402', '1. Design Foundations', 1, 'Visual hierarchy, typography, color theory, and Figma fundamentals'),
    ('ui_stage_2', 'dfa91e34-0bcb-4a2b-a047-25fcb9574402', '2. User Research & Empathy', 2, 'User interviews, persona building, empathy maps, and problem framing'),
    ('ui_stage_3', 'dfa91e34-0bcb-4a2b-a047-25fcb9574402', '3. Wireframing & Prototyping', 3, 'Information architecture, low-fidelity wireframes, and interactive prototypes'),
    ('ui_stage_4', 'dfa91e34-0bcb-4a2b-a047-25fcb9574402', '4. Design Systems & Accessibility', 4, 'Scalable component libraries, WCAG 2.2 accessibility, and developer handoff'),
    ('ui_stage_5', 'dfa91e34-0bcb-4a2b-a047-25fcb9574402', '5. Usability Testing & Iteration', 5, 'Heuristic evaluations, moderated testing sessions, and UX metrics'),
    ('ui_stage_6', 'dfa91e34-0bcb-4a2b-a047-25fcb9574402', '6. Capstone Projects & Portfolio', 6, 'In-depth case studies solving real user problems from research to prototype'),
    ('ui_stage_7', 'dfa91e34-0bcb-4a2b-a047-25fcb9574402', '7. Career & Interview Preparation', 7, 'Portfolio reviews, whiteboard design challenges, and design critique prep'),
    ('cy_stage_1', 'b5753c72-dbc6-46ec-9e46-e9bdef29256e', '1. Networking & Linux Foundations', 1, 'TCP/IP, OSI model, Linux administration, and security fundamentals'),
    ('cy_stage_2', 'b5753c72-dbc6-46ec-9e46-e9bdef29256e', '2. Cryptography & Perimeter Defense', 2, 'Applied encryption, PKI, IAM, and network security controls'),
    ('cy_stage_3', 'b5753c72-dbc6-46ec-9e46-e9bdef29256e', '3. Vulnerability & Application Security', 3, 'Vulnerability scanning, OWASP Top 10, and endpoint defense'),
    ('cy_stage_4', 'b5753c72-dbc6-46ec-9e46-e9bdef29256e', '4. SOC Operations & Incident Response', 4, 'SIEM platforms, threat hunting, log forensics, and incident handling'),
    ('cy_stage_5', 'b5753c72-dbc6-46ec-9e46-e9bdef29256e', '5. Practical Security Projects', 5, 'Hands-on defensive labs, threat investigation reports, and security automation'),
    ('cy_stage_6', 'b5753c72-dbc6-46ec-9e46-e9bdef29256e', '6. Placement & Certification Preparation', 6, 'CompTIA Security+ concepts, technical SOC interview drills, and scenario tests'),
    ('cl_stage_1', '63440f4a-5c2b-4705-8525-7a5f61b18967', '1. Systems & Cloud Foundations', 1, 'Linux server administration, cloud concepts, and virtual networking'),
    ('cl_stage_2', '63440f4a-5c2b-4705-8525-7a5f61b18967', '2. Core Public Cloud Platforms', 2, 'Compute, object storage, managed databases, and cloud IAM'),
    ('cl_stage_3', '63440f4a-5c2b-4705-8525-7a5f61b18967', '3. Containers & Kubernetes Orchestration', 3, 'Docker containerization, Kubernetes clusters, services, and ingress'),
    ('cl_stage_4', '63440f4a-5c2b-4705-8525-7a5f61b18967', '4. Infrastructure as Code & CI/CD', 4, 'Terraform automation, GitHub Actions pipelines, and cloud observability'),
    ('cl_stage_5', '63440f4a-5c2b-4705-8525-7a5f61b18967', '5. Production Cloud Projects', 5, 'Deploying resilient, scalable architectures with automated pipelines'),
    ('cl_stage_6', '63440f4a-5c2b-4705-8525-7a5f61b18967', '6. Placement & Cloud Certification Preparation', 6, 'Cloud scenario interviews, troubleshooting tests, and certification review')
ON CONFLICT (id) DO UPDATE SET
    roadmap_id = EXCLUDED.roadmap_id,
    stage_name = EXCLUDED.stage_name,
    stage_order = EXCLUDED.stage_order,
    subtitle = EXCLUDED.subtitle;

-- 3. Upsert 125 Career-Specific Roadmap Tasks (Exact Catalog TEXT IDs)
INSERT INTO public.roadmap_tasks (id, stage_id, title, description, skill_category, estimated_hours, resource_type, task_order)
VALUES
    ('t1_1', 'stage_1', 'Linear Algebra: Vectors, Matrices & Eigenvalues', 'Key mathematical foundations for neural weights', 'Mathematics', 12, 'Course', 1),
    ('t1_2', 'stage_1', 'Calculus: Partial Derivatives & Gradient Descent', 'Understand how cost functions are minimized', 'Mathematics', 10, 'Course', 2),
    ('t1_3', 'stage_1', 'Git & GitHub: Branching, PRs, and Collaboration', 'Industry-standard code versioning practices', 'Git & GitHub', 6, 'Course', 3),
    ('t1_4', 'stage_1', 'Linux CLI: Shell Scripting & Environment Configuration', 'Basic bash commands, ssh, and permissions', 'DevOps Tools', 5, 'Course', 4),
    ('t2_1', 'stage_2', 'Python Core: OOP, Generators, and Decorators', 'Writing modular and clean production Python', 'Python', 14, 'Course', 1),
    ('t2_2', 'stage_2', 'NumPy & Vectorized Matrix Computations', 'High-performance arrays and mathematical operations', 'Python', 8, 'Course', 2),
    ('t2_3', 'stage_2', 'Pandas: Data Cleaning, Wrangling & Aggregation', 'Manipulating real-world datasets with missing values', 'Python', 10, 'Course', 3),
    ('t2_4', 'stage_2', 'Data Structures: HashMaps, Graphs & Heaps', 'Mastering key interview structures in Python', 'Python', 16, 'Course', 4),
    ('t3_1', 'stage_3', 'SQL: Complex Joins, Window Functions & CTEs', 'Extracting training data from relational databases', 'SQL', 12, 'Course', 1),
    ('t3_2', 'stage_3', 'Supervised ML: Regression, Decision Trees & Random Forests', 'Scikit-Learn modeling and hyperparameter tuning', 'Machine Learning', 18, 'Course', 2),
    ('t3_3', 'stage_3', 'Unsupervised Learning: K-Means & PCA Dimensionality', 'Clustering analysis and feature reduction', 'Machine Learning', 10, 'Course', 3),
    ('t3_4', 'stage_3', 'Model Evaluation: Precision, Recall, ROC-AUC, and F1', 'Avoiding overfitting and data leakage', 'Machine Learning', 8, 'Course', 4),
    ('t4_1', 'stage_4', 'Deep Learning Fundamentals with PyTorch', 'Building multi-layer perceptrons and backprop', 'Deep Learning', 20, 'Course', 1),
    ('t4_2', 'stage_4', 'Computer Vision: Convolutional Networks (CNNs)', 'Image classification, transfer learning with ResNet', 'Deep Learning', 16, 'Course', 2),
    ('t4_3', 'stage_4', 'NLP & Transformers: Attention Mechanisms & BERT', 'Tokenization, embeddings, and fine-tuning Hugging Face', 'Generative AI', 22, 'Course', 3),
    ('t4_4', 'stage_4', 'MLOps: Dockerizing Models & FastApi Endpoints', 'Packaging ML inference services for deployment', 'MLOps', 14, 'Course', 4),
    ('t5_1', 'stage_5', 'Capstone 1: Multi-Modal Sentiment & Vision Classifier', 'End-to-end PyTorch pipeline with FastAPI backend', 'Projects', 25, 'Course', 1),
    ('t5_2', 'stage_5', 'Capstone 2: RAG Pipeline with Vector DB (FAISS/Pinecone)', 'Interactive document query assistant with Gemini API', 'Projects', 30, 'Course', 2),
    ('t5_3', 'stage_5', 'Open Source Contribution & GitHub Portfolio README', 'Showcase clear diagrams, benchmarks, and live demo links', 'Projects', 10, 'Course', 3),
    ('t6_1', 'stage_6', 'Targeted Technical Resume tailored for AI Roles', 'Action-driven bullet points with quantified results', 'Placement Prep', 8, 'Course', 1),
    ('t6_2', 'stage_6', 'Solve 50 LeetCode Medium Problems (Blind 75)', 'Arrays, Two Pointers, Dynamic Programming, and Trees', 'Placement Prep', 35, 'Course', 2),
    ('t6_3', 'stage_6', 'Machine Learning System Design Interview Mock', 'Recommendation systems, latency budgets, and caching', 'Placement Prep', 12, 'Course', 3),
    ('t6_4', 'stage_6', 'HR & Behavioral Interview STAR Method Practice', 'Preparing answers for leadership, conflict, and teamwork', 'Placement Prep', 6, 'Course', 4),
    ('sd_1', 's1', 'Computer Systems & Memory Architecture', 'Stack vs Heap, pointers, and memory cycles', 'Foundation', 10, 'Course', 1),
    ('sd_2', 's1', 'Git & GitHub Workflow Mastery', 'Rebase, merge conflicts, and releases', 'Git & GitHub', 8, 'Course', 2),
    ('sd_3', 's2', 'Kotlin Object-Oriented & Functional Concepts', 'Null safety, extensions, and lambdas', 'Kotlin', 14, 'Course', 1),
    ('sd_4', 's2', 'Data Structures: Trees, Graphs & Dynamic Programming', 'LeetCode interview essentials', 'Data Structures', 20, 'Course', 2),
    ('sd_5', 's3', 'RESTful API Design & Best Practices', 'HTTP verbs, status codes, and auth headers', 'Backend APIs', 12, 'Course', 1),
    ('sd_6', 's3', 'Relational Database Design with PostgreSQL', 'Normalization, indexes, and queries', 'SQL', 14, 'Course', 2),
    ('sd_7', 's4', 'Kotlin Coroutines & Reactive Flow', 'Asynchronous multithreaded programming', 'Concurrency', 16, 'Course', 1),
    ('sd_8', 's4', 'System Design: Load Balancers & Redis Caching', 'Designing scalable distributed architectures', 'System Design', 18, 'Course', 2),
    ('sd_9', 's5', 'Full-Stack Mobile or Web Application', 'Authentication, database sync, and modern UI', 'Projects', 30, 'Course', 1),
    ('sd_10', 's6', '100 DSA Problems & Mock Technical Interviews', 'Mastering technical coding rounds', 'Placement Prep', 35, 'Course', 1),
    ('da_1', 'da1', 'Descriptive & Inferential Statistics', 'Mean, median, standard deviation, hypothesis testing', 'Statistics', 12, 'Course', 1),
    ('da_2', 'da1', 'Advanced Excel: VLOOKUP, XLOOKUP & Pivot Tables', 'Business reporting with spreadsheet models', 'Excel', 8, 'Course', 2),
    ('da_3', 'da2', 'Python Basics & Pandas DataFrames', 'Data manipulation and aggregations', 'Python', 14, 'Course', 1),
    ('da_4', 'da2', 'Matplotlib & Seaborn Data Visualization', 'Histograms, scatterplots, heatmaps', 'Python', 10, 'Course', 2),
    ('da_5', 'da3', 'Advanced SQL: CTEs, Window Functions & Subqueries', 'Extracting insights from enterprise databases', 'SQL', 16, 'Course', 1),
    ('da_6', 'da4', 'Power BI / Tableau Interactive Dashboards', 'KPI cards, drill-down charts, and filters', 'BI Tools', 18, 'Course', 1),
    ('da_7', 'da5', 'E-Commerce Customer Churn Analysis', 'Full report with recommendations', 'Projects', 20, 'Course', 1),
    ('da_8', 'da6', 'SQL Assessment Prep & Business Case Mock Interviews', 'Live query coding tests', 'Placement Prep', 20, 'Course', 1),
    ('ds_1_1', 'ds_stage_1', 'Python Programming Core & OOP', 'Variables, control flow, functions, OOP, and data structures', 'Python', 14, 'Course', 1),
    ('ds_1_2', 'ds_stage_1', 'Git & GitHub for Data Science', 'Version control, branching, PRs, and collaborative Jupyter projects', 'Git & GitHub', 8, 'Course', 2),
    ('ds_1_3', 'ds_stage_1', 'Algorithmic Thinking & Basic Data Structures', 'Lists, dictionaries, sets, recursion, and time complexity', 'Problem Solving', 12, 'Course', 3),
    ('ds_2_1', 'ds_stage_2', 'Linear Algebra Essentials for Data Science', 'Vectors, matrix operations, eigenvalues, and transformations', 'Mathematics', 14, 'Course', 1),
    ('ds_2_2', 'ds_stage_2', 'Multivariable Calculus & Gradients', 'Partial derivatives, chain rule, and loss gradient optimization', 'Mathematics', 10, 'Course', 2),
    ('ds_2_3', 'ds_stage_2', 'Descriptive & Inferential Statistics', 'Central tendency, variance, confidence intervals, and hypothesis testing', 'Statistics', 16, 'Course', 3),
    ('ds_2_4', 'ds_stage_2', 'Probability Theory & Bayes'' Theorem', 'Conditional probability, random variables, and Bayesian inference', 'Statistics', 12, 'Course', 4),
    ('ds_3_1', 'ds_stage_3', 'NumPy Vectorized Numerical Computing', 'Multi-dimensional arrays, indexing, and mathematical vectorization', 'Data Handling', 10, 'Course', 1),
    ('ds_3_2', 'ds_stage_3', 'Pandas Data Cleaning & Wrangling', 'DataFrames, handling null values, reshaping, and aggregations', 'Data Handling', 16, 'Course', 2),
    ('ds_3_3', 'ds_stage_3', 'Exploratory Data Analysis (EDA) & Visualization', 'Matplotlib and Seaborn distributions, heatmaps, and outlier detection', 'Data Visualization', 12, 'Course', 3),
    ('ds_3_4', 'ds_stage_3', 'Relational SQL Queries for Analytics', 'Multi-table joins, subqueries, aggregations, and window functions', 'SQL', 14, 'Course', 4),
    ('ds_4_1', 'ds_stage_4', 'Feature Engineering & Preprocessing', 'Scaling, one-hot encoding, imputation, and feature extraction', 'Machine Learning', 12, 'Course', 1),
    ('ds_4_2', 'ds_stage_4', 'Supervised Learning: Regression', 'Linear, Ridge, and Lasso regression with MSE and R² evaluation', 'Machine Learning', 14, 'Course', 2),
    ('ds_4_3', 'ds_stage_4', 'Supervised Learning: Classification', 'Logistic regression, Decision Trees, SVM, and Naive Bayes', 'Machine Learning', 16, 'Course', 3),
    ('ds_4_4', 'ds_stage_4', 'Unsupervised Learning: Clustering & PCA', 'K-Means, hierarchical clustering, and PCA dimensionality reduction', 'Machine Learning', 14, 'Course', 4),
    ('ds_4_5', 'ds_stage_4', 'Model Evaluation & Cross-Validation', 'Confusion matrix, Precision, Recall, F1, ROC-AUC, and k-fold validation', 'Machine Learning', 10, 'Course', 5),
    ('ds_5_1', 'ds_stage_5', 'Ensemble Methods: Random Forests & XGBoost', 'Bagging vs boosting, gradient boosting, and feature importance', 'Advanced ML', 18, 'Course', 1),
    ('ds_5_2', 'ds_stage_5', 'Hyperparameter Tuning & Pipelines', 'GridSearchCV, RandomizedSearchCV, and Scikit-Learn pipelines', 'Advanced ML', 12, 'Course', 2),
    ('ds_5_3', 'ds_stage_5', 'Time Series Analysis & Forecasting', 'Stationarity, autocorrelation, trend decomposition, and ARIMA modeling', 'Advanced ML', 14, 'Course', 3),
    ('ds_5_4', 'ds_stage_5', 'Natural Language Processing (NLP) Basics', 'Tokenization, TF-IDF, word embeddings, and text classification', 'NLP', 16, 'Course', 4),
    ('ds_6_1', 'ds_stage_6', 'Customer Churn Prediction & Retention System', 'Complete pipeline: EDA, feature engineering, XGBoost, and business strategy', 'Projects', 24, 'Course', 1),
    ('ds_6_2', 'ds_stage_6', 'Retail Sales Demand Forecasting Dashboard', 'Multi-store time series forecasting with interactive Streamlit dashboard', 'Projects', 24, 'Course', 2),
    ('ds_6_3', 'ds_stage_6', 'Data Science GitHub Portfolio & Technical README', 'Clean Jupyter notebooks, reproducible code, and architecture diagrams', 'Portfolio', 12, 'Course', 3),
    ('ds_7_1', 'ds_stage_7', 'Targeted Data Science Resume & GitHub Showcase', 'Quantified business impact, ML metrics, and tailored competencies', 'Placement Prep', 8, 'Course', 1),
    ('ds_7_2', 'ds_stage_7', 'Live SQL Query Coding Interview Practice', 'LeetCode SQL 50: subqueries, window functions, and self-joins under time limits', 'Placement Prep', 20, 'Course', 2),
    ('ds_7_3', 'ds_stage_7', 'Machine Learning & Statistics Mock Interviews', 'Explain bias-variance tradeoff, p-values, gradient descent, and trade-offs', 'Placement Prep', 16, 'Course', 3),
    ('ds_7_4', 'ds_stage_7', 'Python Algorithmic Problem Solving for DS', 'Practice common interview algorithms: arrays, hashing, and two pointers', 'Placement Prep', 18, 'Course', 4),
    ('ui_1_1', 'ui_stage_1', 'Visual Design Principles & Gestalt Theory', 'Visual hierarchy, alignment, contrast, balance, and white space', 'Visual Design', 12, 'Course', 1),
    ('ui_1_2', 'ui_stage_1', 'Typography & Color Theory for Interfaces', 'Modular type scales, font pairings, contrast ratios, and palettes', 'Visual Design', 10, 'Course', 2),
    ('ui_1_3', 'ui_stage_1', 'Figma Mastery: Auto-Layout & Components', 'Responsive layouts, component properties, variants, and design tokens', 'Figma', 16, 'Course', 3),
    ('ui_2_1', 'ui_stage_2', 'User Research Methods & Interviews', 'Qualitative interviews, surveys, and competitive analysis benchmarking', 'User Research', 14, 'Course', 1),
    ('ui_2_2', 'ui_stage_2', 'User Personas & Empathy Mapping', 'Synthesize research into user archetypes, pain points, and motivations', 'User Research', 10, 'Course', 2),
    ('ui_2_3', 'ui_stage_2', 'Customer Journey Mapping & User Flows', 'End-to-end user journeys, friction identification, and decision flowcharts', 'Information Architecture', 12, 'Course', 3),
    ('ui_3_1', 'ui_stage_3', 'Information Architecture & Card Sorting', 'Navigation hierarchy, taxonomy, site maps, and content groupings', 'Information Architecture', 10, 'Course', 1),
    ('ui_3_2', 'ui_stage_3', 'Low-Fidelity Wireframing & Iteration', 'Grayscale wireframes focusing purely on structure, flow, and clarity', 'Wireframing', 14, 'Course', 2),
    ('ui_3_3', 'ui_stage_3', 'High-Fidelity Prototyping & Micro-Interactions', 'Smart animate transitions, interactive component states, and gestures', 'Prototyping', 18, 'Course', 3),
    ('ui_4_1', 'ui_stage_4', 'Design Systems & Token Architecture', 'Atomic design: color tokens, typography scales, buttons, inputs, and modals', 'Design Systems', 20, 'Course', 1),
    ('ui_4_2', 'ui_stage_4', 'Digital Accessibility (WCAG 2.2 Standards)', 'Color contrast (4.5:1), 48dp touch targets, screen reader tags, and focus states', 'Accessibility', 12, 'Course', 2),
    ('ui_4_3', 'ui_stage_4', 'Developer Handoff & Design Specs', 'Inspect mode, redlining, asset exporting, responsiveness specs, and tokens', 'Handoff', 10, 'Course', 3),
    ('ui_5_1', 'ui_stage_5', 'Nielsen Norman 10 Usability Heuristics', 'Interface evaluation against usability heuristics with severity scoring', 'Usability', 12, 'Course', 1),
    ('ui_5_2', 'ui_stage_5', 'Usability Testing Sessions & Task Analysis', 'Moderating user testing, measuring completion rates, and feedback analysis', 'Usability', 16, 'Course', 2),
    ('ui_5_3', 'ui_stage_5', 'A/B Testing & Behavioral UX Metrics', 'Hypothesis formulation, heatmaps, click rates, and funnel drop-off analysis', 'UX Analytics', 12, 'Course', 3),
    ('ui_6_1', 'ui_stage_6', 'Mobile App Redesign Case Study', 'UX audit, user research, wireframe iterations, and tested high-fi prototype', 'Portfolio', 25, 'Course', 1),
    ('ui_6_2', 'ui_stage_6', 'Responsive SaaS Web Dashboard Design', 'Multi-panel analytics platform with design system, light/dark themes, and charts', 'Portfolio', 25, 'Course', 2),
    ('ui_6_3', 'ui_stage_6', 'UX Case Study Presentation & Storytelling', 'Structure compelling case studies: problem, research, iterations, and metrics', 'Portfolio', 15, 'Course', 3),
    ('ui_7_1', 'ui_stage_7', 'Polished Product Design Portfolio Website', 'Curate 3 detailed case studies with process breakdowns and interactive prototypes', 'Placement Prep', 15, 'Course', 1),
    ('ui_7_2', 'ui_stage_7', 'Whiteboard Design Challenge Framework', 'Live 45-minute design challenges: questions, user needs, layout, and trade-offs', 'Placement Prep', 16, 'Course', 2),
    ('ui_7_3', 'ui_stage_7', 'App Critique & Behavioral Interview Practice', 'Deconstruct popular apps live, discuss UX decisions, and practice cross-team talk', 'Placement Prep', 12, 'Course', 3),
    ('cy_1_1', 'cy_stage_1', 'Computer Networking & Protocols', 'TCP/IP, OSI model, DNS, DHCP, HTTP/S, subnetting, and packet capture', 'Networking', 14, 'Course', 1),
    ('cy_1_2', 'cy_stage_1', 'Linux Systems Administration & Shell Security', 'File permissions, systemctl, process monitoring, and bash automation scripts', 'Linux', 12, 'Course', 2),
    ('cy_1_3', 'cy_stage_1', 'Core Cybersecurity Concepts & Threat Models', 'CIA Triad, threat actor profiles, attack vectors, and defense-in-depth', 'Security Foundations', 10, 'Course', 3),
    ('cy_2_1', 'cy_stage_2', 'Applied Cryptography & PKI Infrastructure', 'Symmetric/asymmetric encryption, SHA-256 hashing, and SSL/TLS certificates', 'Cryptography', 14, 'Course', 1),
    ('cy_2_2', 'cy_stage_2', 'Identity & Access Management (IAM) and Zero Trust', 'Authentication vs authorization, RBAC, Multi-Factor Authentication, and Zero Trust', 'IAM', 12, 'Course', 2),
    ('cy_2_3', 'cy_stage_2', 'Network Defense: Firewalls, IDS/IPS & Proxies', 'Stateful inspection, Snort IDS rules, reverse proxies, and VPN tunnels', 'Network Defense', 16, 'Course', 3),
    ('cy_3_1', 'cy_stage_3', 'Vulnerability Scanning & Patch Management', 'Nessus and Nmap scans, CVE analysis, CVSS scoring, and remediation plans', 'Vulnerability Assessment', 14, 'Course', 1),
    ('cy_3_2', 'cy_stage_3', 'Web Application Security (OWASP Top 10)', 'SQL injection, XSS, CSRF, broken authentication, and defense headers', 'Application Security', 18, 'Course', 2),
    ('cy_3_3', 'cy_stage_3', 'Endpoint Detection & Response (EDR)', 'EDR agents, host firewalls, file integrity monitoring, and malware basics', 'Endpoint Security', 12, 'Course', 3),
    ('cy_4_1', 'cy_stage_4', 'SIEM Platforms: Splunk & Elastic Security', 'Syslog ingestion, Windows Event Logs, correlation rules, and SOC dashboards', 'SOC Operations', 18, 'Course', 1),
    ('cy_4_2', 'cy_stage_4', 'Threat Hunting & Forensic Log Analysis', 'Investigating anomalous spikes, brute force attempts, and log correlation', 'Threat Hunting', 16, 'Course', 2),
    ('cy_4_3', 'cy_stage_4', 'Incident Response Lifecycle (NIST SP 800-61)', 'Preparation, detection, containment, eradication, recovery, and post-mortems', 'Incident Response', 14, 'Course', 3),
    ('cy_5_1', 'cy_stage_5', 'Active Directory Security Home Lab', 'Configure enterprise AD, simulate credential attacks, and apply defensive GPOs', 'Projects', 24, 'Course', 1),
    ('cy_5_2', 'cy_stage_5', 'SOC Incident Triage & Forensic Report', 'Document simulated ransomware/phishing incident with IOCs and executive summary', 'Projects', 20, 'Course', 2),
    ('cy_5_3', 'cy_stage_5', 'Security Automation with Python', 'Python scripts to query threat intelligence APIs, parse logs, and flag malicious IPs', 'Projects', 16, 'Course', 3),
    ('cy_6_1', 'cy_stage_6', 'CompTIA Security+ / CEH Core Knowledge Review', 'Comprehensive review of attack types, network ports, crypto, and compliance', 'Placement Prep', 20, 'Course', 1),
    ('cy_6_2', 'cy_stage_6', 'SOC Analyst Scenario Mock Interviews', 'Live troubleshooting scenarios: suspicious email attachments, lateral movement', 'Placement Prep', 16, 'Course', 2),
    ('cy_6_3', 'cy_stage_6', 'Cybersecurity Resume Polish & CTF Walkthroughs', 'Showcase TryHackMe badges, documented lab reports, and STAR behavioral answers', 'Placement Prep', 10, 'Course', 3),
    ('cl_1_1', 'cl_stage_1', 'Linux Server Administration & Shell Scripting', 'Systemctl, cron jobs, permissions, SSH keys, and bash automation scripts', 'Linux', 14, 'Course', 1),
    ('cl_1_2', 'cl_stage_1', 'Cloud Computing Models & Shared Responsibility', 'IaaS, PaaS, SaaS, cloud economics, fault tolerance, and security boundaries', 'Cloud Foundations', 10, 'Course', 2),
    ('cl_1_3', 'cl_stage_1', 'Cloud Networking: VPCs, Subnets & Routing', 'Virtual Private Clouds, public/private subnets, Internet/NAT gateways, route tables', 'Networking', 16, 'Course', 3),
    ('cl_2_1', 'cl_stage_2', 'Cloud Compute: Virtual Machines & Auto-Scaling', 'AWS EC2 / GCP Compute, AMIs, Auto-Scaling Groups, and Elastic Load Balancing', 'Cloud Compute', 16, 'Course', 1),
    ('cl_2_2', 'cl_stage_2', 'Cloud Storage & Database Services', 'Object storage (S3), block volumes, managed SQL (RDS), and NoSQL services', 'Storage & DB', 14, 'Course', 2),
    ('cl_2_3', 'cl_stage_2', 'Cloud IAM & Least Privilege Security', 'IAM users, roles, policies, service accounts, and KMS encryption keys', 'Cloud Security', 12, 'Course', 3),
    ('cl_3_1', 'cl_stage_3', 'Docker Containerization & Image Optimization', 'Multi-stage Dockerfiles, layer caching, volumes, and Docker Compose networks', 'Containers', 16, 'Course', 1),
    ('cl_3_2', 'cl_stage_3', 'Kubernetes Cluster Architecture & Core Objects', 'Control plane, Pods, Deployments, ReplicaSets, and Services (ClusterIP, NodePort)', 'Kubernetes', 20, 'Course', 2),
    ('cl_3_3', 'cl_stage_3', 'Kubernetes Config, Storage & Ingress', 'ConfigMaps, Secrets, PersistentVolumes, Ingress controllers, and Helm charts', 'Kubernetes', 16, 'Course', 3),
    ('cl_4_1', 'cl_stage_4', 'Infrastructure as Code with Terraform', 'Declarative HCL, state file locking, input variables, outputs, and modules', 'Terraform', 20, 'Course', 1),
    ('cl_4_2', 'cl_stage_4', 'CI/CD Automation with GitHub Actions', 'Automated build/test workflows, Docker image push to ECR, and cloud deploy', 'CI/CD', 16, 'Course', 2),
    ('cl_4_3', 'cl_stage_4', 'Cloud Monitoring & Observability', 'CloudWatch / Prometheus metrics, Grafana dashboards, and automated alert alerts', 'Observability', 14, 'Course', 3),
    ('cl_5_1', 'cl_stage_5', 'Scalable Multi-Tier Web Application on Kubernetes', 'Microservice architecture with auto-scaling, ingress, TLS, and health checks', 'Projects', 26, 'Course', 1),
    ('cl_5_2', 'cl_stage_5', 'Terraform-Automated Cloud Infrastructure Pipeline', 'Multi-region VPC, database cluster, and monitoring stack provisioned via IaC', 'Projects', 24, 'Course', 2),
    ('cl_5_3', 'cl_stage_5', 'Cloud Architecture Portfolio & Live Deployment Demos', 'Architecture diagrams, cost optimization analysis, and disaster recovery plan', 'Portfolio', 10, 'Course', 3),
    ('cl_6_1', 'cl_stage_6', 'Cloud Solutions Architect Scenario Mock Interviews', 'Design for high availability, fault tolerance, multi-AZ failover, and cost optimization', 'Placement Prep', 18, 'Course', 1),
    ('cl_6_2', 'cl_stage_6', 'Linux & Cloud Incident Troubleshooting Drills', 'Diagnose broken routing, CrashLoopBackOff containers, and permission issues', 'Placement Prep', 16, 'Course', 2),
    ('cl_6_3', 'cl_stage_6', 'Cloud Engineer Resume Review & Behavioral STAR Method', 'Highlight cloud certifications (AWS SAA / CKA), uptime metrics, and teamwork', 'Placement Prep', 8, 'Course', 3)
ON CONFLICT (id) DO UPDATE SET
    stage_id = EXCLUDED.stage_id,
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    skill_category = EXCLUDED.skill_category,
    estimated_hours = EXCLUDED.estimated_hours,
    resource_type = EXCLUDED.resource_type,
    task_order = EXCLUDED.task_order;

-- ==============================================================================
-- 4. POST-MIGRATION VERIFICATION QUERIES
-- ==============================================================================

-- Verification 1: Total stages count (Expected: 44)
SELECT COUNT(*) AS total_stages FROM public.roadmap_stages;

-- Verification 2: Total tasks count (Expected: 125)
SELECT COUNT(*) AS total_tasks FROM public.roadmap_tasks;

-- Verification 3: Stages and Tasks count per Career
-- Expected:
--   aiml_engineer   : 6 stages, 23 tasks
--   software_dev    : 6 stages, 10 tasks
--   data_analyst    : 6 stages,  8 tasks
--   data_scientist  : 7 stages, 27 tasks
--   uiux_designer   : 7 stages, 21 tasks
--   cyber_analyst   : 6 stages, 18 tasks
--   cloud_engineer  : 6 stages, 18 tasks
SELECT
    r.career_id,
    r.id AS roadmap_id,
    COUNT(DISTINCT s.id) AS stage_count,
    COUNT(t.id) AS task_count
FROM public.roadmaps r
LEFT JOIN public.roadmap_stages s ON s.roadmap_id = r.id
LEFT JOIN public.roadmap_tasks t ON t.stage_id = s.id
GROUP BY r.career_id, r.id
ORDER BY r.career_id;

-- Verification 4: Check for orphan stages (Expected: 0)
SELECT s.id AS orphan_stage_id, s.roadmap_id
FROM public.roadmap_stages s
LEFT JOIN public.roadmaps r ON s.roadmap_id = r.id
WHERE r.id IS NULL;

-- Verification 5: Check for orphan tasks (Expected: 0)
SELECT t.id AS orphan_task_id, t.stage_id
FROM public.roadmap_tasks t
LEFT JOIN public.roadmap_stages s ON t.stage_id = s.id
WHERE s.id IS NULL;

-- Verification 6: Check for duplicate task IDs (Expected: 0)
SELECT id, COUNT(*) AS occurrences
FROM public.roadmap_tasks
GROUP BY id
HAVING COUNT(*) > 1;

-- Verification 7: Check existence of specific Stage 7 rows (Expected: 2 rows)
SELECT id, roadmap_id, stage_name, stage_order, subtitle
FROM public.roadmap_stages
WHERE id IN ('ds_stage_7', 'ui_stage_7')
ORDER BY id;

-- Verification 8: Check user_task_progress integrity (untouched)
SELECT COUNT(*) AS user_progress_rows FROM public.user_task_progress;
