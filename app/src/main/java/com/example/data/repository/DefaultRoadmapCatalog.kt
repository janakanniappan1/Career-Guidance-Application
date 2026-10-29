package com.example.data.repository

import com.example.domain.model.CareerRoadmap
import com.example.domain.model.RoadmapStage
import com.example.domain.model.RoadmapTask
import com.example.util.CareerCanonicalUtil

/**
 * Standard career roadmap templates for offline operation, unauthenticated exploration,
 * and graceful fallback when Supabase tables are unavailable or require elevated authentication.
 */
object DefaultRoadmapCatalog {

    fun getStandardRoadmap(careerId: String): CareerRoadmap? {
        val canonicalId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId) ?: return null

        return when (canonicalId) {
            "aiml_engineer" -> createDefaultAimlRoadmap()
            "software_dev" -> createSoftwareDevRoadmap()
            "data_analyst" -> createDataAnalystRoadmap()
            "data_scientist" -> createDataScientistRoadmap()
            "uiux_designer" -> createUiuxRoadmap()
            "cyber_analyst" -> createCybersecurityRoadmap()
            "cloud_engineer" -> createCloudEngineerRoadmap()
            else -> null
        }
    }

    private fun createDefaultAimlRoadmap(): CareerRoadmap {
        return CareerRoadmap(
            careerId = "aiml_engineer",
            careerTitle = "AI / Machine Learning Engineer",
            stages = listOf(
                RoadmapStage(
                    id = "stage_1",
                    stageName = "1. Foundation",
                    order = 1,
                    subtitle = "Mathematical grounding, Git version control, and Linux basics",
                    tasks = listOf(
                        RoadmapTask("t1_1", "Linear Algebra: Vectors, Matrices & Eigenvalues", "Key mathematical foundations for neural weights", false, "Mathematics", 12),
                        RoadmapTask("t1_2", "Calculus: Partial Derivatives & Gradient Descent", "Understand how cost functions are minimized", false, "Mathematics", 10),
                        RoadmapTask("t1_3", "Git & GitHub: Branching, PRs, and Collaboration", "Industry-standard code versioning practices", false, "Git & GitHub", 6),
                        RoadmapTask("t1_4", "Linux CLI: Shell Scripting & Environment Configuration", "Basic bash commands, ssh, and permissions", false, "DevOps Tools", 5)
                    )
                ),
                RoadmapStage(
                    id = "stage_2",
                    stageName = "2. Programming",
                    order = 2,
                    subtitle = "Python mastery and algorithmic data structures",
                    tasks = listOf(
                        RoadmapTask("t2_1", "Python Core: OOP, Generators, and Decorators", "Writing modular and clean production Python", false, "Python", 14),
                        RoadmapTask("t2_2", "NumPy & Vectorized Matrix Computations", "High-performance arrays and mathematical operations", false, "Python", 8),
                        RoadmapTask("t2_3", "Pandas: Data Cleaning, Wrangling & Aggregation", "Manipulating real-world datasets with missing values", false, "Python", 10),
                        RoadmapTask("t2_4", "Data Structures: HashMaps, Graphs & Heaps", "Mastering key interview structures in Python", false, "Python", 16)
                    )
                ),
                RoadmapStage(
                    id = "stage_3",
                    stageName = "3. Core Skills",
                    order = 3,
                    subtitle = "Supervised/unsupervised algorithms and relational data",
                    tasks = listOf(
                        RoadmapTask("t3_1", "SQL: Complex Joins, Window Functions & CTEs", "Extracting training data from relational databases", false, "SQL", 12),
                        RoadmapTask("t3_2", "Supervised ML: Regression, Decision Trees & Random Forests", "Scikit-Learn modeling and hyperparameter tuning", false, "Machine Learning", 18),
                        RoadmapTask("t3_3", "Unsupervised Learning: K-Means & PCA Dimensionality", "Clustering analysis and feature reduction", false, "Machine Learning", 10),
                        RoadmapTask("t3_4", "Model Evaluation: Precision, Recall, ROC-AUC, and F1", "Avoiding overfitting and data leakage", false, "Machine Learning", 8)
                    )
                ),
                RoadmapStage(
                    id = "stage_4",
                    stageName = "4. Advanced Skills",
                    order = 4,
                    subtitle = "Deep Learning, PyTorch, Transformers, and MLOps",
                    tasks = listOf(
                        RoadmapTask("t4_1", "Deep Learning Fundamentals with PyTorch", "Building multi-layer perceptrons and backprop", false, "Deep Learning", 20),
                        RoadmapTask("t4_2", "Computer Vision: Convolutional Networks (CNNs)", "Image classification, transfer learning with ResNet", false, "Deep Learning", 16),
                        RoadmapTask("t4_3", "NLP & Transformers: Attention Mechanisms & BERT", "Tokenization, embeddings, and fine-tuning Hugging Face", false, "Generative AI", 22),
                        RoadmapTask("t4_4", "MLOps: Dockerizing Models & FastApi Endpoints", "Packaging ML inference services for deployment", false, "MLOps", 14)
                    )
                ),
                RoadmapStage(
                    id = "stage_5",
                    stageName = "5. Projects",
                    order = 5,
                    subtitle = "Portfolio building with end-to-end deployed AI systems",
                    tasks = listOf(
                        RoadmapTask("t5_1", "Capstone 1: Multi-Modal Sentiment & Vision Classifier", "End-to-end PyTorch pipeline with FastAPI backend", false, "Projects", 25),
                        RoadmapTask("t5_2", "Capstone 2: RAG Pipeline with Vector DB (FAISS/Pinecone)", "Interactive document query assistant with Gemini API", false, "Projects", 30),
                        RoadmapTask("t5_3", "Open Source Contribution & GitHub Portfolio README", "Showcase clear diagrams, benchmarks, and live demo links", false, "Projects", 10)
                    )
                ),
                RoadmapStage(
                    id = "stage_6",
                    stageName = "6. Placement Preparation",
                    order = 6,
                    subtitle = "Aptitude, DSA interview coding, resume review & mock rounds",
                    tasks = listOf(
                        RoadmapTask("t6_1", "Targeted Technical Resume tailored for AI Roles", "Action-driven bullet points with quantified results", false, "Placement Prep", 8),
                        RoadmapTask("t6_2", "Solve 50 LeetCode Medium Problems (Blind 75)", "Arrays, Two Pointers, Dynamic Programming, and Trees", false, "Placement Prep", 35),
                        RoadmapTask("t6_3", "Machine Learning System Design Interview Mock", "Recommendation systems, latency budgets, and caching", false, "Placement Prep", 12),
                        RoadmapTask("t6_4", "HR & Behavioral Interview STAR Method Practice", "Preparing answers for leadership, conflict, and teamwork", false, "Placement Prep", 6)
                    )
                )
            )
        )
    }

    private fun createSoftwareDevRoadmap(): CareerRoadmap {
        return CareerRoadmap(
            careerId = "software_dev",
            careerTitle = "Software Developer",
            stages = listOf(
                RoadmapStage("s1", "1. Foundation", 1, "CS basics, Git, and Operating Systems", listOf(
                    RoadmapTask("sd_1", "Computer Systems & Memory Architecture", "Stack vs Heap, pointers, and memory cycles", false, "Foundation", 10),
                    RoadmapTask("sd_2", "Git & GitHub Workflow Mastery", "Rebase, merge conflicts, and releases", false, "Git & GitHub", 8)
                )),
                RoadmapStage("s2", "2. Programming", 2, "Modern Kotlin & Java Core", listOf(
                    RoadmapTask("sd_3", "Kotlin Object-Oriented & Functional Concepts", "Null safety, extensions, and lambdas", false, "Kotlin", 14),
                    RoadmapTask("sd_4", "Data Structures: Trees, Graphs & Dynamic Programming", "LeetCode interview essentials", false, "Data Structures", 20)
                )),
                RoadmapStage("s3", "3. Core Skills", 3, "APIs, Databases & Clean Architecture", listOf(
                    RoadmapTask("sd_5", "RESTful API Design & Best Practices", "HTTP verbs, status codes, and auth headers", false, "Backend APIs", 12),
                    RoadmapTask("sd_6", "Relational Database Design with PostgreSQL", "Normalization, indexes, and queries", false, "SQL", 14)
                )),
                RoadmapStage("s4", "4. Advanced Skills", 4, "Microservices, Caching & Concurrency", listOf(
                    RoadmapTask("sd_7", "Kotlin Coroutines & Reactive Flow", "Asynchronous multithreaded programming", false, "Concurrency", 16),
                    RoadmapTask("sd_8", "System Design: Load Balancers & Redis Caching", "Designing scalable distributed architectures", false, "System Design", 18)
                )),
                RoadmapStage("s5", "5. Projects", 5, "Full-Stack Production Application", listOf(
                    RoadmapTask("sd_9", "Full-Stack Mobile or Web Application", "Authentication, database sync, and modern UI", false, "Projects", 30)
                )),
                RoadmapStage("s6", "6. Placement Preparation", 6, "Coding contests, resume & mock interviews", listOf(
                    RoadmapTask("sd_10", "100 DSA Problems & Mock Technical Interviews", "Mastering technical coding rounds", false, "Placement Prep", 35)
                ))
            )
        )
    }

    private fun createDataAnalystRoadmap(): CareerRoadmap {
        return CareerRoadmap(
            careerId = "data_analyst",
            careerTitle = "Data Analyst",
            stages = listOf(
                RoadmapStage("da1", "1. Foundation", 1, "Statistics and Spreadsheets", listOf(
                    RoadmapTask("da_1", "Descriptive & Inferential Statistics", "Mean, median, standard deviation, hypothesis testing", false, "Statistics", 12),
                    RoadmapTask("da_2", "Advanced Excel: VLOOKUP, XLOOKUP & Pivot Tables", "Business reporting with spreadsheet models", false, "Excel", 8)
                )),
                RoadmapStage("da2", "2. Programming", 2, "Python for Data Analysis", listOf(
                    RoadmapTask("da_3", "Python Basics & Pandas DataFrames", "Data manipulation and aggregations", false, "Python", 14),
                    RoadmapTask("da_4", "Matplotlib & Seaborn Data Visualization", "Histograms, scatterplots, heatmaps", false, "Python", 10)
                )),
                RoadmapStage("da3", "3. Core Skills", 3, "SQL & Relational Databases", listOf(
                    RoadmapTask("da_5", "Advanced SQL: CTEs, Window Functions & Subqueries", "Extracting insights from enterprise databases", false, "SQL", 16)
                )),
                RoadmapStage("da4", "4. Advanced Skills", 4, "BI Dashboards & Storytelling", listOf(
                    RoadmapTask("da_6", "Power BI / Tableau Interactive Dashboards", "KPI cards, drill-down charts, and filters", false, "BI Tools", 18)
                )),
                RoadmapStage("da5", "5. Projects", 5, "Real-world Business Analytics Case Studies", listOf(
                    RoadmapTask("da_7", "E-Commerce Customer Churn Analysis", "Full report with recommendations", false, "Projects", 20)
                )),
                RoadmapStage("da6", "6. Placement Preparation", 6, "SQL interview tests & case interview prep", listOf(
                    RoadmapTask("da_8", "SQL Assessment Prep & Business Case Mock Interviews", "Live query coding tests", false, "Placement Prep", 20)
                ))
            )
        )
    }

    private fun createDataScientistRoadmap(): CareerRoadmap {
        return CareerRoadmap(
            careerId = "data_scientist",
            careerTitle = "Data Scientist",
            stages = listOf(
                RoadmapStage(
                    id = "ds_stage_1",
                    stageName = "1. Foundations & Python",
                    order = 1,
                    subtitle = "Python programming fundamentals, Git version control, and problem solving",
                    tasks = listOf(
                        RoadmapTask("ds_1_1", "Python Programming Core & OOP", "Variables, control flow, functions, OOP, and data structures", false, "Python", 14),
                        RoadmapTask("ds_1_2", "Git & GitHub for Data Science", "Version control, branching, PRs, and collaborative Jupyter projects", false, "Git & GitHub", 8),
                        RoadmapTask("ds_1_3", "Algorithmic Thinking & Basic Data Structures", "Lists, dictionaries, sets, recursion, and time complexity", false, "Problem Solving", 12)
                    )
                ),
                RoadmapStage(
                    id = "ds_stage_2",
                    stageName = "2. Mathematics & Statistics",
                    order = 2,
                    subtitle = "Probability theory, inferential statistics, linear algebra, and calculus",
                    tasks = listOf(
                        RoadmapTask("ds_2_1", "Linear Algebra Essentials for Data Science", "Vectors, matrix operations, eigenvalues, and transformations", false, "Mathematics", 14),
                        RoadmapTask("ds_2_2", "Multivariable Calculus & Gradients", "Partial derivatives, chain rule, and loss gradient optimization", false, "Mathematics", 10),
                        RoadmapTask("ds_2_3", "Descriptive & Inferential Statistics", "Central tendency, variance, confidence intervals, and hypothesis testing", false, "Statistics", 16),
                        RoadmapTask("ds_2_4", "Probability Theory & Bayes' Theorem", "Conditional probability, random variables, and Bayesian inference", false, "Statistics", 12)
                    )
                ),
                RoadmapStage(
                    id = "ds_stage_3",
                    stageName = "3. Data Handling & SQL",
                    order = 3,
                    subtitle = "NumPy arrays, Pandas wrangling, exploratory analysis, and SQL queries",
                    tasks = listOf(
                        RoadmapTask("ds_3_1", "NumPy Vectorized Numerical Computing", "Multi-dimensional arrays, indexing, and mathematical vectorization", false, "Data Handling", 10),
                        RoadmapTask("ds_3_2", "Pandas Data Cleaning & Wrangling", "DataFrames, handling null values, reshaping, and aggregations", false, "Data Handling", 16),
                        RoadmapTask("ds_3_3", "Exploratory Data Analysis (EDA) & Visualization", "Matplotlib and Seaborn distributions, heatmaps, and outlier detection", false, "Data Visualization", 12),
                        RoadmapTask("ds_3_4", "Relational SQL Queries for Analytics", "Multi-table joins, subqueries, aggregations, and window functions", false, "SQL", 14)
                    )
                ),
                RoadmapStage(
                    id = "ds_stage_4",
                    stageName = "4. Machine Learning Foundations",
                    order = 4,
                    subtitle = "Supervised and unsupervised algorithms, feature engineering, and validation",
                    tasks = listOf(
                        RoadmapTask("ds_4_1", "Feature Engineering & Preprocessing", "Scaling, one-hot encoding, imputation, and feature extraction", false, "Machine Learning", 12),
                        RoadmapTask("ds_4_2", "Supervised Learning: Regression", "Linear, Ridge, and Lasso regression with MSE and R² evaluation", false, "Machine Learning", 14),
                        RoadmapTask("ds_4_3", "Supervised Learning: Classification", "Logistic regression, Decision Trees, SVM, and Naive Bayes", false, "Machine Learning", 16),
                        RoadmapTask("ds_4_4", "Unsupervised Learning: Clustering & PCA", "K-Means, hierarchical clustering, and PCA dimensionality reduction", false, "Machine Learning", 14),
                        RoadmapTask("ds_4_5", "Model Evaluation & Cross-Validation", "Confusion matrix, Precision, Recall, F1, ROC-AUC, and k-fold validation", false, "Machine Learning", 10)
                    )
                ),
                RoadmapStage(
                    id = "ds_stage_5",
                    stageName = "5. Advanced Data Science",
                    order = 5,
                    subtitle = "Ensemble learning, hyperparameter tuning, time series, and NLP",
                    tasks = listOf(
                        RoadmapTask("ds_5_1", "Ensemble Methods: Random Forests & XGBoost", "Bagging vs boosting, gradient boosting, and feature importance", false, "Advanced ML", 18),
                        RoadmapTask("ds_5_2", "Hyperparameter Tuning & Pipelines", "GridSearchCV, RandomizedSearchCV, and Scikit-Learn pipelines", false, "Advanced ML", 12),
                        RoadmapTask("ds_5_3", "Time Series Analysis & Forecasting", "Stationarity, autocorrelation, trend decomposition, and ARIMA modeling", false, "Advanced ML", 14),
                        RoadmapTask("ds_5_4", "Natural Language Processing (NLP) Basics", "Tokenization, TF-IDF, word embeddings, and text classification", false, "NLP", 16)
                    )
                ),
                RoadmapStage(
                    id = "ds_stage_6",
                    stageName = "6. Real-World Projects",
                    order = 6,
                    subtitle = "End-to-end data science projects from raw data to business recommendations",
                    tasks = listOf(
                        RoadmapTask("ds_6_1", "Customer Churn Prediction & Retention System", "Complete pipeline: EDA, feature engineering, XGBoost, and business strategy", false, "Projects", 24),
                        RoadmapTask("ds_6_2", "Retail Sales Demand Forecasting Dashboard", "Multi-store time series forecasting with interactive Streamlit dashboard", false, "Projects", 24),
                        RoadmapTask("ds_6_3", "Data Science GitHub Portfolio & Technical README", "Clean Jupyter notebooks, reproducible code, and architecture diagrams", false, "Portfolio", 12)
                    )
                ),
                RoadmapStage(
                    id = "ds_stage_7",
                    stageName = "7. Placement Preparation",
                    order = 7,
                    subtitle = "Technical coding, SQL assessments, ML conceptual rounds, and mock interviews",
                    tasks = listOf(
                        RoadmapTask("ds_7_1", "Targeted Data Science Resume & GitHub Showcase", "Quantified business impact, ML metrics, and tailored competencies", false, "Placement Prep", 8),
                        RoadmapTask("ds_7_2", "Live SQL Query Coding Interview Practice", "LeetCode SQL 50: subqueries, window functions, and self-joins under time limits", false, "Placement Prep", 20),
                        RoadmapTask("ds_7_3", "Machine Learning & Statistics Mock Interviews", "Explain bias-variance tradeoff, p-values, gradient descent, and trade-offs", false, "Placement Prep", 16),
                        RoadmapTask("ds_7_4", "Python Algorithmic Problem Solving for DS", "Practice common interview algorithms: arrays, hashing, and two pointers", false, "Placement Prep", 18)
                    )
                )
            )
        )
    }

    private fun createUiuxRoadmap(): CareerRoadmap {
        return CareerRoadmap(
            careerId = "uiux_designer",
            careerTitle = "UI/UX Designer",
            stages = listOf(
                RoadmapStage(
                    id = "ui_stage_1",
                    stageName = "1. Design Foundations",
                    order = 1,
                    subtitle = "Visual hierarchy, typography, color theory, and Figma fundamentals",
                    tasks = listOf(
                        RoadmapTask("ui_1_1", "Visual Design Principles & Gestalt Theory", "Visual hierarchy, alignment, contrast, balance, and white space", false, "Visual Design", 12),
                        RoadmapTask("ui_1_2", "Typography & Color Theory for Interfaces", "Modular type scales, font pairings, contrast ratios, and palettes", false, "Visual Design", 10),
                        RoadmapTask("ui_1_3", "Figma Mastery: Auto-Layout & Components", "Responsive layouts, component properties, variants, and design tokens", false, "Figma", 16)
                    )
                ),
                RoadmapStage(
                    id = "ui_stage_2",
                    stageName = "2. User Research & Empathy",
                    order = 2,
                    subtitle = "User interviews, persona building, empathy maps, and problem framing",
                    tasks = listOf(
                        RoadmapTask("ui_2_1", "User Research Methods & Interviews", "Qualitative interviews, surveys, and competitive analysis benchmarking", false, "User Research", 14),
                        RoadmapTask("ui_2_2", "User Personas & Empathy Mapping", "Synthesize research into user archetypes, pain points, and motivations", false, "User Research", 10),
                        RoadmapTask("ui_2_3", "Customer Journey Mapping & User Flows", "End-to-end user journeys, friction identification, and decision flowcharts", false, "Information Architecture", 12)
                    )
                ),
                RoadmapStage(
                    id = "ui_stage_3",
                    stageName = "3. Wireframing & Prototyping",
                    order = 3,
                    subtitle = "Information architecture, low-fidelity wireframes, and interactive prototypes",
                    tasks = listOf(
                        RoadmapTask("ui_3_1", "Information Architecture & Card Sorting", "Navigation hierarchy, taxonomy, site maps, and content groupings", false, "Information Architecture", 10),
                        RoadmapTask("ui_3_2", "Low-Fidelity Wireframing & Iteration", "Grayscale wireframes focusing purely on structure, flow, and clarity", false, "Wireframing", 14),
                        RoadmapTask("ui_3_3", "High-Fidelity Prototyping & Micro-Interactions", "Smart animate transitions, interactive component states, and gestures", false, "Prototyping", 18)
                    )
                ),
                RoadmapStage(
                    id = "ui_stage_4",
                    stageName = "4. Design Systems & Accessibility",
                    order = 4,
                    subtitle = "Scalable component libraries, WCAG 2.2 accessibility, and developer handoff",
                    tasks = listOf(
                        RoadmapTask("ui_4_1", "Design Systems & Token Architecture", "Atomic design: color tokens, typography scales, buttons, inputs, and modals", false, "Design Systems", 20),
                        RoadmapTask("ui_4_2", "Digital Accessibility (WCAG 2.2 Standards)", "Color contrast (4.5:1), 48dp touch targets, screen reader tags, and focus states", false, "Accessibility", 12),
                        RoadmapTask("ui_4_3", "Developer Handoff & Design Specs", "Inspect mode, redlining, asset exporting, responsiveness specs, and tokens", false, "Handoff", 10)
                    )
                ),
                RoadmapStage(
                    id = "ui_stage_5",
                    stageName = "5. Usability Testing & Iteration",
                    order = 5,
                    subtitle = "Heuristic evaluations, moderated testing sessions, and UX metrics",
                    tasks = listOf(
                        RoadmapTask("ui_5_1", "Nielsen Norman 10 Usability Heuristics", "Interface evaluation against usability heuristics with severity scoring", false, "Usability", 12),
                        RoadmapTask("ui_5_2", "Usability Testing Sessions & Task Analysis", "Moderating user testing, measuring completion rates, and feedback analysis", false, "Usability", 16),
                        RoadmapTask("ui_5_3", "A/B Testing & Behavioral UX Metrics", "Hypothesis formulation, heatmaps, click rates, and funnel drop-off analysis", false, "UX Analytics", 12)
                    )
                ),
                RoadmapStage(
                    id = "ui_stage_6",
                    stageName = "6. Capstone Projects & Portfolio",
                    order = 6,
                    subtitle = "In-depth case studies solving real user problems from research to prototype",
                    tasks = listOf(
                        RoadmapTask("ui_6_1", "Mobile App Redesign Case Study", "UX audit, user research, wireframe iterations, and tested high-fi prototype", false, "Portfolio", 25),
                        RoadmapTask("ui_6_2", "Responsive SaaS Web Dashboard Design", "Multi-panel analytics platform with design system, light/dark themes, and charts", false, "Portfolio", 25),
                        RoadmapTask("ui_6_3", "UX Case Study Presentation & Storytelling", "Structure compelling case studies: problem, research, iterations, and metrics", false, "Portfolio", 15)
                    )
                ),
                RoadmapStage(
                    id = "ui_stage_7",
                    stageName = "7. Career & Interview Preparation",
                    order = 7,
                    subtitle = "Portfolio reviews, whiteboard design challenges, and design critique prep",
                    tasks = listOf(
                        RoadmapTask("ui_7_1", "Polished Product Design Portfolio Website", "Curate 3 detailed case studies with process breakdowns and interactive prototypes", false, "Placement Prep", 15),
                        RoadmapTask("ui_7_2", "Whiteboard Design Challenge Framework", "Live 45-minute design challenges: questions, user needs, layout, and trade-offs", false, "Placement Prep", 16),
                        RoadmapTask("ui_7_3", "App Critique & Behavioral Interview Practice", "Deconstruct popular apps live, discuss UX decisions, and practice cross-team talk", false, "Placement Prep", 12)
                    )
                )
            )
        )
    }

    private fun createCybersecurityRoadmap(): CareerRoadmap {
        return CareerRoadmap(
            careerId = "cyber_analyst",
            careerTitle = "Cybersecurity Analyst",
            stages = listOf(
                RoadmapStage(
                    id = "cy_stage_1",
                    stageName = "1. Networking & Linux Foundations",
                    order = 1,
                    subtitle = "TCP/IP, OSI model, Linux administration, and security fundamentals",
                    tasks = listOf(
                        RoadmapTask("cy_1_1", "Computer Networking & Protocols", "TCP/IP, OSI model, DNS, DHCP, HTTP/S, subnetting, and packet capture", false, "Networking", 14),
                        RoadmapTask("cy_1_2", "Linux Systems Administration & Shell Security", "File permissions, systemctl, process monitoring, and bash automation scripts", false, "Linux", 12),
                        RoadmapTask("cy_1_3", "Core Cybersecurity Concepts & Threat Models", "CIA Triad, threat actor profiles, attack vectors, and defense-in-depth", false, "Security Foundations", 10)
                    )
                ),
                RoadmapStage(
                    id = "cy_stage_2",
                    stageName = "2. Cryptography & Perimeter Defense",
                    order = 2,
                    subtitle = "Applied encryption, PKI, IAM, and network security controls",
                    tasks = listOf(
                        RoadmapTask("cy_2_1", "Applied Cryptography & PKI Infrastructure", "Symmetric/asymmetric encryption, SHA-256 hashing, and SSL/TLS certificates", false, "Cryptography", 14),
                        RoadmapTask("cy_2_2", "Identity & Access Management (IAM) and Zero Trust", "Authentication vs authorization, RBAC, Multi-Factor Authentication, and Zero Trust", false, "IAM", 12),
                        RoadmapTask("cy_2_3", "Network Defense: Firewalls, IDS/IPS & Proxies", "Stateful inspection, Snort IDS rules, reverse proxies, and VPN tunnels", false, "Network Defense", 16)
                    )
                ),
                RoadmapStage(
                    id = "cy_stage_3",
                    stageName = "3. Vulnerability & Application Security",
                    order = 3,
                    subtitle = "Vulnerability scanning, OWASP Top 10, and endpoint defense",
                    tasks = listOf(
                        RoadmapTask("cy_3_1", "Vulnerability Scanning & Patch Management", "Nessus and Nmap scans, CVE analysis, CVSS scoring, and remediation plans", false, "Vulnerability Assessment", 14),
                        RoadmapTask("cy_3_2", "Web Application Security (OWASP Top 10)", "SQL injection, XSS, CSRF, broken authentication, and defense headers", false, "Application Security", 18),
                        RoadmapTask("cy_3_3", "Endpoint Detection & Response (EDR)", "EDR agents, host firewalls, file integrity monitoring, and malware basics", false, "Endpoint Security", 12)
                    )
                ),
                RoadmapStage(
                    id = "cy_stage_4",
                    stageName = "4. SOC Operations & Incident Response",
                    order = 4,
                    subtitle = "SIEM platforms, threat hunting, log forensics, and incident handling",
                    tasks = listOf(
                        RoadmapTask("cy_4_1", "SIEM Platforms: Splunk & Elastic Security", "Syslog ingestion, Windows Event Logs, correlation rules, and SOC dashboards", false, "SOC Operations", 18),
                        RoadmapTask("cy_4_2", "Threat Hunting & Forensic Log Analysis", "Investigating anomalous spikes, brute force attempts, and log correlation", false, "Threat Hunting", 16),
                        RoadmapTask("cy_4_3", "Incident Response Lifecycle (NIST SP 800-61)", "Preparation, detection, containment, eradication, recovery, and post-mortems", false, "Incident Response", 14)
                    )
                ),
                RoadmapStage(
                    id = "cy_stage_5",
                    stageName = "5. Practical Security Projects",
                    order = 5,
                    subtitle = "Hands-on defensive labs, threat investigation reports, and security automation",
                    tasks = listOf(
                        RoadmapTask("cy_5_1", "Active Directory Security Home Lab", "Configure enterprise AD, simulate credential attacks, and apply defensive GPOs", false, "Projects", 24),
                        RoadmapTask("cy_5_2", "SOC Incident Triage & Forensic Report", "Document simulated ransomware/phishing incident with IOCs and executive summary", false, "Projects", 20),
                        RoadmapTask("cy_5_3", "Security Automation with Python", "Python scripts to query threat intelligence APIs, parse logs, and flag malicious IPs", false, "Projects", 16)
                    )
                ),
                RoadmapStage(
                    id = "cy_stage_6",
                    stageName = "6. Placement & Certification Preparation",
                    order = 6,
                    subtitle = "CompTIA Security+ concepts, technical SOC interview drills, and scenario tests",
                    tasks = listOf(
                        RoadmapTask("cy_6_1", "CompTIA Security+ / CEH Core Knowledge Review", "Comprehensive review of attack types, network ports, crypto, and compliance", false, "Placement Prep", 20),
                        RoadmapTask("cy_6_2", "SOC Analyst Scenario Mock Interviews", "Live troubleshooting scenarios: suspicious email attachments, lateral movement", false, "Placement Prep", 16),
                        RoadmapTask("cy_6_3", "Cybersecurity Resume Polish & CTF Walkthroughs", "Showcase TryHackMe badges, documented lab reports, and STAR behavioral answers", false, "Placement Prep", 10)
                    )
                )
            )
        )
    }

    private fun createCloudEngineerRoadmap(): CareerRoadmap {
        return CareerRoadmap(
            careerId = "cloud_engineer",
            careerTitle = "Cloud Engineer",
            stages = listOf(
                RoadmapStage(
                    id = "cl_stage_1",
                    stageName = "1. Systems & Cloud Foundations",
                    order = 1,
                    subtitle = "Linux server administration, cloud concepts, and virtual networking",
                    tasks = listOf(
                        RoadmapTask("cl_1_1", "Linux Server Administration & Shell Scripting", "Systemctl, cron jobs, permissions, SSH keys, and bash automation scripts", false, "Linux", 14),
                        RoadmapTask("cl_1_2", "Cloud Computing Models & Shared Responsibility", "IaaS, PaaS, SaaS, cloud economics, fault tolerance, and security boundaries", false, "Cloud Foundations", 10),
                        RoadmapTask("cl_1_3", "Cloud Networking: VPCs, Subnets & Routing", "Virtual Private Clouds, public/private subnets, Internet/NAT gateways, route tables", false, "Networking", 16)
                    )
                ),
                RoadmapStage(
                    id = "cl_stage_2",
                    stageName = "2. Core Public Cloud Platforms",
                    order = 2,
                    subtitle = "Compute, object storage, managed databases, and cloud IAM",
                    tasks = listOf(
                        RoadmapTask("cl_2_1", "Cloud Compute: Virtual Machines & Auto-Scaling", "AWS EC2 / GCP Compute, AMIs, Auto-Scaling Groups, and Elastic Load Balancing", false, "Cloud Compute", 16),
                        RoadmapTask("cl_2_2", "Cloud Storage & Database Services", "Object storage (S3), block volumes, managed SQL (RDS), and NoSQL services", false, "Storage & DB", 14),
                        RoadmapTask("cl_2_3", "Cloud IAM & Least Privilege Security", "IAM users, roles, policies, service accounts, and KMS encryption keys", false, "Cloud Security", 12)
                    )
                ),
                RoadmapStage(
                    id = "cl_stage_3",
                    stageName = "3. Containers & Kubernetes Orchestration",
                    order = 3,
                    subtitle = "Docker containerization, Kubernetes clusters, services, and ingress",
                    tasks = listOf(
                        RoadmapTask("cl_3_1", "Docker Containerization & Image Optimization", "Multi-stage Dockerfiles, layer caching, volumes, and Docker Compose networks", false, "Containers", 16),
                        RoadmapTask("cl_3_2", "Kubernetes Cluster Architecture & Core Objects", "Control plane, Pods, Deployments, ReplicaSets, and Services (ClusterIP, NodePort)", false, "Kubernetes", 20),
                        RoadmapTask("cl_3_3", "Kubernetes Config, Storage & Ingress", "ConfigMaps, Secrets, PersistentVolumes, Ingress controllers, and Helm charts", false, "Kubernetes", 16)
                    )
                ),
                RoadmapStage(
                    id = "cl_stage_4",
                    stageName = "4. Infrastructure as Code & CI/CD",
                    order = 4,
                    subtitle = "Terraform automation, GitHub Actions pipelines, and cloud observability",
                    tasks = listOf(
                        RoadmapTask("cl_4_1", "Infrastructure as Code with Terraform", "Declarative HCL, state file locking, input variables, outputs, and modules", false, "Terraform", 20),
                        RoadmapTask("cl_4_2", "CI/CD Automation with GitHub Actions", "Automated build/test workflows, Docker image push to ECR, and cloud deploy", false, "CI/CD", 16),
                        RoadmapTask("cl_4_3", "Cloud Monitoring & Observability", "CloudWatch / Prometheus metrics, Grafana dashboards, and automated alert alerts", false, "Observability", 14)
                    )
                ),
                RoadmapStage(
                    id = "cl_stage_5",
                    stageName = "5. Production Cloud Projects",
                    order = 5,
                    subtitle = "Deploying resilient, scalable architectures with automated pipelines",
                    tasks = listOf(
                        RoadmapTask("cl_5_1", "Scalable Multi-Tier Web Application on Kubernetes", "Microservice architecture with auto-scaling, ingress, TLS, and health checks", false, "Projects", 26),
                        RoadmapTask("cl_5_2", "Terraform-Automated Cloud Infrastructure Pipeline", "Multi-region VPC, database cluster, and monitoring stack provisioned via IaC", false, "Projects", 24),
                        RoadmapTask("cl_5_3", "Cloud Architecture Portfolio & Live Deployment Demos", "Architecture diagrams, cost optimization analysis, and disaster recovery plan", false, "Portfolio", 10)
                    )
                ),
                RoadmapStage(
                    id = "cl_stage_6",
                    stageName = "6. Placement & Cloud Certification Preparation",
                    order = 6,
                    subtitle = "Cloud scenario interviews, troubleshooting tests, and certification review",
                    tasks = listOf(
                        RoadmapTask("cl_6_1", "Cloud Solutions Architect Scenario Mock Interviews", "Design for high availability, fault tolerance, multi-AZ failover, and cost optimization", false, "Placement Prep", 18),
                        RoadmapTask("cl_6_2", "Linux & Cloud Incident Troubleshooting Drills", "Diagnose broken routing, CrashLoopBackOff containers, and permission issues", false, "Placement Prep", 16),
                        RoadmapTask("cl_6_3", "Cloud Engineer Resume Review & Behavioral STAR Method", "Highlight cloud certifications (AWS SAA / CKA), uptime metrics, and teamwork", false, "Placement Prep", 8)
                    )
                )
            )
        )
    }
}
