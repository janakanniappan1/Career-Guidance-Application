package com.example.data.repository

import com.example.domain.model.InterviewQuestion
import com.example.domain.model.PracticeConstants
import com.example.domain.model.PracticeProblem
import com.example.domain.model.ProjectItem
import com.example.domain.model.QuizQuestion
import com.example.util.CareerCanonicalUtil

/**
 * Verified fallback catalog for Practice Problems, Speed Quizzes, and Interview Questions.
 *
 * Guarantees:
 * - High-quality, academically appropriate placement questions.
 * - Accurate answer indices, clear explanations, and deterministic difficulty ratings.
 * - Career-specific questions seamlessly merged with general aptitude & DSA.
 */
object DefaultPracticeCatalog {

    val GLOBAL_PRACTICE_PROBLEMS: List<PracticeProblem> = listOf(
        PracticeProblem(
            id = "dsa_01",
            title = "Two Sum & Hash Map Lookup",
            category = "Coding",
            difficulty = "Easy",
            description = "You are given an integer array nums and an integer target. Your task is to find the indices of the two distinct elements whose values add up to target. You may assume that each input has exactly one valid pair of numbers, and you cannot use the same element twice.\n\nFor nums = [2, 7, 11, 15] and target = 9, which algorithmic technique correctly solves the problem in optimal O(N) time complexity and O(N) auxiliary space?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Arrays & Hashing",
            exampleInput = "nums = [2, 7, 11, 15], target = 9",
            exampleOutput = "Index pair [i, j] such that nums[i] + nums[j] == target",
            explanation = "Iterating once through nums while storing visited elements and their indices in a Hash Map allows complement lookups (target - nums[i]) in O(1) average time. At index 1 (value 7), complement 9 - 7 = 2 is found at index 0, yielding [0, 1] in O(N) total time.",
            constraints = "2 <= nums.length <= 10^4, -10^9 <= nums[i] <= 10^9. Exactly one valid pair exists.",
            hint = "As you iterate through the array, calculate complement = target - nums[i] and check if that complement already exists in your hash map.",
            options = listOf(
                "Single pass with a Hash Map storing visited values and indices to look up complements in O(N) time",
                "Brute-force nested loops checking all possible index pairs (i, j) in O(N^2) time",
                "Sort the array in O(N log N) time and use binary search to locate original indices",
                "Recursive backtracking generating all 2^N subsets until a sum matches target"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "dsa_02",
            title = "Reverse a Singly Linked List",
            category = "Coding",
            difficulty = "Easy",
            description = "You are given the head of a singly linked list containing sequential values 1 -> 2 -> 3 -> 4 -> 5 -> NULL. You need to reverse the direction of all pointer links in place so that the list becomes 5 -> 4 -> 3 -> 2 -> 1 -> NULL and return the new head node.\n\nWhich pointer manipulation technique achieves this in-place reversal in O(N) linear time using strictly O(1) auxiliary memory?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Linked Lists",
            exampleInput = "head = [1, 2, 3, 4, 5]",
            exampleOutput = "Reversed list structure with pointers directed toward predecessors",
            explanation = "By maintaining three pointers (prev, curr, nextNode), we iteratively save curr.next, redirect curr.next to prev, advance prev to curr, and advance curr to nextNode. When curr reaches NULL, prev points to the new head (node 5), taking O(N) time and O(1) extra space.",
            constraints = "Number of nodes in list is in the range [0, 5000]. In-place pointer modifications only.",
            hint = "Maintain three pointer references: prev (initialized to null), curr (initialized to head), and a temporary nextNode to save curr.next before redirecting curr.next = prev.",
            options = listOf(
                "Copy all node values into an auxiliary array, reverse the array, and overwrite node values",
                "Iterate through nodes using three pointers (prev, curr, next) to reverse curr.next pointers in O(1) space",
                "Allocate a brand new linked list and prepend copies of every node in reverse order",
                "Swap values between pairs of nodes recursively without modifying node next references"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "dsa_03",
            title = "Longest Substring Without Repeating Characters",
            category = "Coding",
            difficulty = "Medium",
            description = "Given a string s = \"abcabcbb\", you need to find the length of the longest contiguous substring that contains no duplicate characters. For example, in \"abcabcbb\", the longest non-repeating substrings are \"abc\", \"bca\", and \"cab\", each with a length of 3.\n\nWhich algorithmic strategy finds the maximum length of a non-repeating substring in optimal O(N) time complexity?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Sliding Window",
            exampleInput = "s = \"abcabcbb\"",
            exampleOutput = "Length of longest contiguous substring containing unique characters",
            explanation = "A sliding window bounded by left and right pointers tracks the active substring. A Hash Map stores each character's last observed index. When character s[right] has been seen inside the current window, left is updated to max(left, lastSeen[s[right]] + 1). This processes each character at most twice, achieving O(N) time and O(min(N, AlphabetSize)) space.",
            constraints = "0 <= s.length <= 5 * 10^4. s consists of English letters, digits, symbols, and spaces.",
            hint = "Maintain a dynamic window [left, right] with a Hash Set or Hash Map storing the most recent index of each character. When a duplicate is encountered at right, advance the left pointer past its previous occurrence.",
            options = listOf(
                "Generate all O(N^2) possible substrings and verify character uniqueness for each in O(N^3) total time",
                "Sort all characters in the string alphabetically and count the longest continuous non-duplicate block",
                "Use a sliding window with two pointers and a Hash Map/Set tracking character positions in O(N) time",
                "Count character global frequencies with a single integer array and subtract duplicate counts"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "dsa_04",
            title = "Binary Tree Level Order Traversal",
            category = "Coding",
            difficulty = "Medium",
            description = "You are given the root of a binary tree: [3, 9, 20, null, null, 15, 7]. You are required to traverse the tree level by level from top to bottom, grouping node values horizontally from left to right for each depth level, producing [[3], [9, 20], [15, 7]].\n\nWhich data structure and traversal pattern guarantees that all nodes at depth d are processed before any nodes at depth d+1?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Trees & Graphs",
            exampleInput = "root = [3, 9, 20, null, null, 15, 7]",
            exampleOutput = "Nested list containing node values grouped by tree depth",
            explanation = "Breadth-First Search (BFS) using a FIFO queue naturally processes nodes level by level. By recording levelSize = queue.size() before processing each level, we dequeue all levelSize nodes into a sublist while enqueuing their children for the next level. This runs in O(N) time and O(W) space where W is the maximum tree width.",
            constraints = "The number of nodes in the tree is in the range [0, 2000]. -1000 <= Node.val <= 1000.",
            hint = "Use a First-In-First-Out (FIFO) Queue. At the start of each level loop, capture the current queue size, poll that exact number of nodes for the level list, and enqueue their non-null left and right children.",
            options = listOf(
                "Depth-First Search using a LIFO Stack to traverse all left descendants before right descendants",
                "In-Order Traversal recording node values into a sorted Red-Black balanced binary tree",
                "Topological sort on node memory pointers using a priority queue",
                "Breadth-First Search using a FIFO Queue and recording queue size at each level boundary in O(N) time"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "dsa_05",
            title = "Climbing Stairs (Dynamic Programming)",
            category = "Technical",
            difficulty = "Easy",
            description = "You are climbing a staircase that has n steps to reach the top. Each time you take a step, you can climb either 1 step or 2 steps. Given n = 3 steps, there are 3 distinct ways to reach the top: (1+1+1), (1+2), and (2+1).\n\nWhich dynamic programming recurrence relation and base cases correctly compute the total number of distinct ways to climb n stairs in O(n) time and O(1) space?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Dynamic Programming",
            exampleInput = "n = 4",
            exampleOutput = "Total count of distinct step combinations reaching step n",
            explanation = "The number of ways to reach step n is the sum of ways to reach step n-1 and step n-2: dp[n] = dp[n-1] + dp[n-2]. Base cases are dp[1] = 1 and dp[2] = 2. This represents the Fibonacci sequence and can be computed iteratively in O(n) time and O(1) space by tracking only the two previous values.",
            constraints = "1 <= n <= 45. The answer fits within a standard 32-bit signed integer.",
            hint = "To arrive at step i, you could have only jumped from step i-1 (by taking 1 step) or from step i-2 (by taking 2 steps).",
            options = listOf(
                "dp[i] = dp[i-1] + dp[i-2] with base cases dp[1] = 1, dp[2] = 2",
                "dp[i] = dp[i-1] * dp[i-2] with base cases dp[1] = 1, dp[2] = 2",
                "dp[i] = 2 * dp[i-1] - 1 with base case dp[1] = 1",
                "Exhaustive unmemoized recursion branches yielding O(2^n) exponential complexity"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "aiml_01",
            title = "Vector Dot Product & Cosine Similarity",
            category = "Technical",
            difficulty = "Easy",
            description = "A semantic search engine encodes document and query text into dense embedding vectors A and B of dimension D = 1536. To rank document relevance independent of vector magnitudes, the system computes the cosine of the angle between vectors A and B.\n\nWhich mathematical formula represents the Cosine Similarity between vector A and vector B?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "aiml_engineer",
            topic = "Linear Algebra & Embeddings",
            exampleInput = "A = [1.0, 2.0, 3.0], B = [1.0, 2.0, 3.0]",
            exampleOutput = "Scalar similarity score in range [-1.0, 1.0]",
            explanation = "Cosine Similarity is defined as cos(theta) = (A · B) / (||A||_2 * ||B||_2) = (sum(A[i]*B[i])) / (sqrt(sum(A[i]^2)) * sqrt(sum(B[i]^2))). It measures orientation rather than magnitude, bounded in [-1.0, 1.0].",
            constraints = "Vectors A and B are non-zero vectors in R^D where 1 <= D <= 1536.",
            hint = "Cosine similarity measures the normalized inner product: the dot product of the vectors divided by the product of their Euclidean L2 norms.",
            options = listOf(
                "Euclidean Distance: sqrt(sum((A[i] - B[i])^2))",
                "Normalized Dot Product: (A · B) / (||A||_2 * ||B||_2)",
                "Manhattan L1 Distance: sum(|A[i] - B[i]|)",
                "Matrix Determinant: det(A (x) B^T)"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "aiml_02",
            title = "Cross-Entropy Loss & Softmax Computation",
            category = "Technical",
            difficulty = "Medium",
            description = "A neural network classifier outputs uncalibrated real-valued logits z = [2.0, 1.0, 0.1] for C = 3 mutually exclusive classes. The model needs to convert these logits into a valid probability distribution p and compute the loss for a ground-truth one-hot label y where class 0 is the true target (y = [1, 0, 0]).\n\nWhich combination of activation function and loss function is the industry standard for multi-class classification, and why?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "aiml_engineer",
            topic = "Deep Learning Fundamentals",
            exampleInput = "Classification with C classes, unnormalized output logits z = [z_1, ..., z_C]",
            exampleOutput = "Normalized class probability vector and scalar cross-entropy loss",
            explanation = "Softmax converts logits into probabilities via p_i = exp(z_i) / sum(exp(z_j)). Categorical Cross-Entropy loss L = -sum(y_i * log(p_i)) penalizes incorrect predictions logarithmically, maximizing the log-likelihood of the ground-truth class. Its gradient (p - y) is simple and numerically stable during backpropagation.",
            constraints = "Number of classes C >= 2. Probability distribution must sum to 1.0.",
            hint = "Softmax normalizes exponential logits into probabilities that sum to 1. Categorical Cross-Entropy takes the negative natural logarithm of the predicted probability for the true class.",
            options = listOf(
                "Sigmoid activation on each output node with Mean Squared Error (MSE) loss",
                "ReLU activation across output nodes with Hinge Loss",
                "Softmax activation to generate a probability distribution with Categorical Cross-Entropy loss",
                "Softmax activation with Mean Absolute Error (MAE) loss"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "aiml_03",
            title = "K-Means Clustering Convergence",
            category = "Technical",
            difficulty = "Medium",
            description = "You are implementing the standard Lloyd's algorithm for K-Means clustering on an unlabeled dataset of N feature vectors with K target cluster centroids. In each iteration, points are assigned to the nearest centroid and centroids are updated.\n\nWhich mathematical objective function does K-Means iteratively minimize to guarantee convergence to a local minimum?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "aiml_engineer",
            topic = "Unsupervised Learning",
            exampleInput = "K = 3 clusters, N = 1000 2D feature points, convergence threshold epsilon = 1e-4",
            exampleOutput = "Partitioned clusters minimizing intra-cluster centroid variance",
            explanation = "Lloyd's K-Means alternates between the Expectation step (assigning each point to its closest centroid) and the Maximization step (recomputing centroids as the arithmetic mean of assigned points). Each step monotonically decreases the Within-Cluster Sum of Squares (WCSS = sum_k sum_{x in C_k} ||x - mu_k||^2), guaranteeing convergence.",
            constraints = "K >= 1, Euclidean distance metric used for point-centroid assignment.",
            hint = "K-Means minimizes the Within-Cluster Sum of Squares (WCSS), which measures the sum of squared Euclidean distances between points and their assigned cluster centroid.",
            options = listOf(
                "Maximizing between-cluster variance using gradient ascent on silhouette coefficients",
                "Minimizing the Kullback-Leibler divergence between sample clusters and Gaussian priors",
                "Maximizing the convex hull area enclosing each of the K cluster boundaries",
                "Minimizing Within-Cluster Sum of Squares (WCSS) through alternating assignment and mean recalculation"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "aiml_04",
            title = "Convolution Output Dimensions & Weight Sizing",
            category = "Technical",
            difficulty = "Easy",
            description = "A 2D convolutional layer receives an input image volume of dimensions 32 x 32 x 3 (Height x Width x Channels). The layer applies 16 filters, each of size 3 x 3, with a stride of S = 1 and zero padding of P = 0.\n\nWhat are the spatial height, spatial width, and channel depth dimensions (H_out x W_out x C_out) of the resulting output feature map?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "aiml_engineer",
            topic = "Computer Vision & CNNs",
            exampleInput = "Input tensor: 32x32x3, Filters: 16 kernels of size 3x3, Stride: 1, Padding: 0",
            exampleOutput = "Spatial dimensions (Height x Width x Channels) of output feature tensor",
            explanation = "Applying the formula: Height_out = floor((32 - 3 + 2*0) / 1) + 1 = 29 + 1 = 30. Width_out = floor((32 - 3 + 2*0) / 1) + 1 = 30. The number of filters is 16, so the output depth C_out = 16. The final output tensor shape is 30 x 30 x 16.",
            constraints = "Standard 2D convolution arithmetic without dilation (Dilation = 1).",
            hint = "Use the spatial dimension formula: Output = floor((W - K + 2*P) / S) + 1. The output channel depth always equals the number of convolutional filters applied.",
            options = listOf(
                "30 x 30 x 16",
                "32 x 32 x 16",
                "28 x 28 x 3",
                "16 x 16 x 32"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "aiml_05",
            title = "Precision-Recall Curve & Threshold Optimization",
            category = "Technical",
            difficulty = "Medium",
            description = "A machine learning model is deployed in a hospital to detect a rare malignant disease present in only 0.5% of tested patients. Failing to detect a true positive patient (a False Negative) has severe health consequences, while a False Positive results in a harmless follow-up screening.\n\nWhich evaluation metric must the engineering team prioritize and maximize when tuning the classification decision threshold?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "aiml_engineer",
            topic = "Model Evaluation",
            exampleInput = "Binary disease screening dataset with 0.5% positive prevalence and high misdiagnosis risk",
            exampleOutput = "Optimal metric selection for critical False Negative reduction",
            explanation = "Recall (also called Sensitivity) measures the fraction of actual positive cases that were correctly identified by the model: Recall = TP / (TP + FN). When missing a positive case (False Negative) is dangerous or costly, the classification threshold is lowered to maximize Recall.",
            constraints = "Severe class imbalance: Positive class prevalence = 0.5%. High cost for False Negatives.",
            hint = "Recall = True Positives / (True Positives + False Negatives). High recall minimizes False Negatives.",
            options = listOf(
                "Precision exclusively, in order to avoid issuing any false positive alarms",
                "Recall (Sensitivity), in order to minimize False Negatives and capture virtually all disease cases",
                "Raw Overall Accuracy, since a dummy model predicting all negatives achieves 99.5% accuracy",
                "Specificity exclusively, to maximize true negative identification"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "data_03",
            title = "Hypothesis Testing & A/B Test Power Calculation",
            category = "Technical",
            difficulty = "Hard",
            description = "A data science team is designing an online A/B experiment to evaluate a new checkout workflow. The baseline conversion rate is p = 5.0%, and the team wants to detect a minimum detectable effect (MDE) of +1.0% (to 6.0%) with a significance level of alpha = 0.05 (Type I error) and a statistical power of 1 - beta = 0.80.\n\nIn statistical hypothesis testing, what does the statistical power (1 - beta = 0.80) specifically represent?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_scientist",
            topic = "Inferential Statistics",
            exampleInput = "A/B test design parameters: alpha = 0.05, beta = 0.20, statistical power = 1 - beta = 0.80",
            exampleOutput = "Statistical interpretation of experimental power in hypothesis testing",
            explanation = "Statistical power (1 - beta) is the probability of correctly rejecting the null hypothesis when an actual difference or effect exists. A power of 0.80 means the A/B test has an 80% chance of successfully detecting the 1.0% conversion uplift without committing a Type II false-negative error.",
            constraints = "Two-tailed two-sample proportion Z-test under standard normal distribution assumptions.",
            hint = "Beta is the probability of committing a Type II error (failing to reject a false null hypothesis). Therefore, 1 - beta is the probability of correctly detecting a genuine effect.",
            options = listOf(
                "The probability of rejecting the null hypothesis when the null hypothesis is actually true (Type I error)",
                "The percentage of users who must be assigned to the control group during the experiment",
                "The probability of correctly rejecting the null hypothesis when an effect truly exists (1 - Type II error)",
                "The proportion of variance explained by random demographic noise"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "ds_01",
            title = "Linear Regression: Normal Equation vs Gradient Descent",
            category = "Technical",
            difficulty = "Medium",
            description = "You need to train a multiple linear regression model on a dataset containing N = 500,000 training examples and D = 25,000 continuous features. You must choose between the analytical closed-form Normal Equation (theta = (X^T X)^(-1) X^T y) and iterative Batch Gradient Descent.\n\nWhy is iterative Gradient Descent strongly preferred over the analytical Normal Equation for this high-dimensional dataset?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_scientist",
            topic = "Statistical Modeling",
            exampleInput = "Tabular dataset dimensions: N = 500,000 samples, D = 25,000 continuous features",
            exampleOutput = "Computational complexity trade-off between analytical and iterative methods",
            explanation = "The Normal Equation requires computing (X^T X)^(-1), which involves inverting a D x D matrix with computational complexity of O(D^3). For D = 25,000, inverting this matrix is computationally prohibitive and memory-intensive. In contrast, Gradient Descent scales efficiently with O(k * N * D) complexity.",
            constraints = "Feature count D = 25,000. Matrix inversion computational complexity scales with feature count.",
            hint = "Consider the computational cost and memory footprint of calculating the inverse of the (D x D) matrix (X^T X).",
            options = listOf(
                "The Normal Equation cannot find the global minimum for linear regression models",
                "Gradient Descent does not require setting a learning rate hyperparameter",
                "The Normal Equation requires all features to follow a standard Gaussian distribution",
                "The Normal Equation requires inverting a D x D matrix in O(D^3) time, which is prohibitive for D = 25,000"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "ds_02",
            title = "Principal Component Analysis (PCA) & Variance Ratio",
            category = "Technical",
            difficulty = "Medium",
            description = "You have a dataset with 50 correlated numerical features. To reduce dimensionality while retaining at least 90% of the total dataset variance, you perform Principal Component Analysis (PCA) by computing the eigendecomposition of the sample covariance matrix Sigma = (1/N) * X_centered^T X_centered.\n\nHow does PCA construct these new orthogonal principal components and determine their individual importance?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_scientist",
            topic = "Dimensionality Reduction",
            exampleInput = "Mean-centered data matrix X with 50 continuous features across N samples",
            exampleOutput = "Orthogonal projection axes and explained variance proportions",
            explanation = "PCA computes the eigenvectors and eigenvalues of the feature covariance matrix. The eigenvectors define mutually orthogonal directions of maximum variance, and each eigenvalue corresponds to the variance captured by that component. Sorting eigenvalues in descending order lets us select the top k components that exceed the target cumulative variance ratio.",
            constraints = "Features must be mean-centered (and standardized if on different scales) prior to PCA.",
            hint = "The eigenvectors of the covariance matrix represent the orthogonal directions of maximum variance, while their corresponding eigenvalues indicate the amount of variance along each axis.",
            options = listOf(
                "PCA computes orthogonal eigenvectors of the covariance matrix, with eigenvalues quantifying explained variance",
                "PCA selects a subset of the original features without applying any linear transformations",
                "PCA trains an iterative decision tree ensemble to score non-linear feature interactions",
                "PCA maximizes class separation using supervised target label feedback"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "ds_03",
            title = "Feature Scaling: Standardization vs Min-Max Normalization",
            category = "Technical",
            difficulty = "Easy",
            description = "You are preparing a machine learning dataset containing numerical features with varying units and scales. You partition the data into an 80% training set and a 20% test set. You want to apply StandardScaler (z = (x - mu) / sigma) to ensure gradient-based models converge smoothly without introducing data leakage.\n\nWhich workflow correctly applies feature scaling without leaking information from the test set into the model training pipeline?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_scientist",
            topic = "Feature Engineering",
            exampleInput = "Partitioned dataset: X_train (80%), X_test (20%) requiring feature standardization",
            exampleOutput = "Safe parameter fitting and transformation pipeline without data leakage",
            explanation = "To prevent data leakage, scaler statistics (mean mu and standard deviation sigma) must be calculated strictly from X_train using scaler.fit_transform(X_train). The exact same learned parameters are then used to transform X_test using scaler.transform(X_test). Fitting the scaler on the combined dataset or test set leaks distribution knowledge.",
            constraints = "Strict prevention of data leakage between training and testing evaluation partitions.",
            hint = "The parameters (mean mu and standard deviation sigma) must be learned exclusively from the training partition and then applied to transform both datasets.",
            options = listOf(
                "Fit the scaler parameters on the entire combined dataset before performing the train-test split",
                "Fit the scaler exclusively on X_train, then use those learned parameters to transform both X_train and X_test",
                "Fit two separate scalers independently: one on X_train and one on X_test",
                "Scale the target label y while leaving input feature columns completely unstandardized"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "ds_04",
            title = "Random Forest Out-of-Bag (OOB) Error & Feature Importance",
            category = "Technical",
            difficulty = "Medium",
            description = "When training a Random Forest classifier with bootstrap aggregating (bagging) on N training samples, each individual decision tree is trained on a bootstrap sample drawn uniformly with replacement from the N instances.\n\nApproximately what percentage of the original training instances are omitted from a given tree's training set (known as Out-of-Bag samples), and how are they used?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_scientist",
            topic = "Ensemble Methods",
            exampleInput = "Training dataset of size N sampled with replacement for each ensemble tree",
            exampleOutput = "Proportion of omitted instances and their validation utility",
            explanation = "In bootstrap sampling with replacement, the probability of an instance not being chosen in N draws is (1 - 1/N)^N. As N grows, this limit is 1/e ≈ 36.8%. These omitted 'Out-of-Bag' (OOB) samples act as an internal, unbiased validation set for each tree, enabling computation of OOB error without a separate validation holdout.",
            constraints = "Bootstrap sampling drawn uniformly with replacement: P(sample not chosen) = (1 - 1/N)^N -> 1/e.",
            hint = "As N approaches infinity, the probability of a sample not being selected in N draws with replacement converges to 1/e ≈ 0.368.",
            options = listOf(
                "Exactly 50.0% of samples are omitted, which are discarded to reduce model memory usage",
                "Exactly 10.0% of samples are omitted, used exclusively to compute tree depth bounds",
                "Approximately 36.8% (~37%) of samples are omitted, serving as an internal validation set to estimate generalization error",
                "0% of samples are omitted because bootstrap sampling always uses all instances"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "data_01",
            title = "SQL Window Functions: Top N Sales per Region",
            category = "Technical",
            difficulty = "Medium",
            description = "You are given a sales database table sales_records with columns (rep_id, rep_name, region, revenue). Your manager asks for a report showing the top 3 highest-earning sales representatives in each geographic region. If two representatives in the same region have identical revenue, both should receive the same rank without skipping subsequent rank numbers.\n\nWhich SQL window function clause correctly calculates this regional rank?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_analyst",
            topic = "SQL & Query Optimization",
            exampleInput = "Database schema: sales_records(rep_id, rep_name, region, revenue)",
            exampleOutput = "Window partition and ranking expression satisfying the gap-free ranking rule",
            explanation = "DENSE_RANK() OVER (PARTITION BY region ORDER BY revenue DESC) evaluates ranks within each partition (region) ordered by revenue descending. Unlike RANK() which leaves gaps after ties (1, 2, 2, 4), DENSE_RANK() produces consecutive integers (1, 2, 2, 3), ensuring top-3 filtering (WHERE rnk <= 3) functions reliably.",
            constraints = "Standard ANSI SQL: PostgreSQL, BigQuery, Snowflake, and MySQL 8.0+ compliant.",
            hint = "PARTITION BY creates separate ranking groups for each region. DENSE_RANK() assigns consecutive integer ranks without gaps when values tie.",
            options = listOf(
                "ROW_NUMBER() OVER (ORDER BY revenue DESC) GROUP BY region",
                "RANK() OVER (ORDER BY region, revenue DESC)",
                "GROUP BY region, rep_id HAVING revenue = MAX(revenue)",
                "DENSE_RANK() OVER (PARTITION BY region ORDER BY revenue DESC)"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "data_02",
            title = "Missing Value Imputation & Outlier Detection",
            category = "Technical",
            difficulty = "Easy",
            description = "You are analyzing customer annual income data from a retail survey. The income distribution is heavily right-skewed with extreme positive outliers representing billionaire customers, as well as several missing values.\n\nWhich central tendency metric and outlier detection method are most robust and appropriate for this right-skewed distribution?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_analyst",
            topic = "Data Wrangling & Statistics",
            exampleInput = "Right-skewed continuous income distribution with missing records and positive outliers",
            exampleOutput = "Robust statistical metrics for central tendency and anomaly bounds",
            explanation = "In right-skewed data, extreme upper-tail values inflate the arithmetic mean and standard deviation. The Median provides a robust measure of central tendency for imputation, and the Interquartile Range rule (bounds at Q1 - 1.5*IQR and Q3 + 1.5*IQR) identifies outliers without assuming a normal bell curve.",
            constraints = "Asymmetric, heavily skewed univariate distribution with extreme upper-tail outliers.",
            hint = "The arithmetic mean and standard deviation are sensitive to extreme outliers. The median and interquartile range (IQR) are non-parametric and resistant to skew.",
            options = listOf(
                "Impute missing values with the Median and detect outliers using the 1.5 * IQR Interquartile Range method",
                "Impute missing values with the arithmetic Mean and detect outliers using +/- 3 Standard Deviations (Z-score)",
                "Delete all rows containing values above the mean and replace missing values with zero",
                "Impute missing values using the statistical Mode and remove the top 50% of records"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "da_01",
            title = "SQL Rolling Averages & Cumulative Metric Tracking",
            category = "Technical",
            difficulty = "Medium",
            description = "An executive dashboard tracks daily online orders from a daily_sales table with columns (sale_date, daily_revenue). To smooth out weekend fluctuations, the leadership team requests a 7-day rolling average of daily revenue calculated for each day (the average of the current day plus the preceding 6 days).\n\nWhich SQL window frame specification correctly calculates this 7-day moving average?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_analyst",
            topic = "Advanced SQL",
            exampleInput = "Schema: daily_sales(sale_date DATE, daily_revenue NUMERIC) ordered sequentially",
            exampleOutput = "Window frame specification spanning current day and 6 preceding rows",
            explanation = "The window frame ROWS BETWEEN 6 PRECEDING AND CURRENT ROW instructs the database engine to include the current row and the 6 prior rows (7 total daily values) when computing AVG(daily_revenue).",
            constraints = "Continuous daily time series ordered chronologically by sale_date.",
            hint = "A 7-day rolling window consists of 7 total rows: the current row plus the 6 preceding rows in chronological order.",
            options = listOf(
                "AVG(daily_revenue) OVER (ORDER BY sale_date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)",
                "AVG(daily_revenue) OVER (ORDER BY sale_date ROWS BETWEEN 6 PRECEDING AND CURRENT ROW)",
                "AVG(daily_revenue) OVER (ORDER BY sale_date ROWS BETWEEN 7 PRECEDING AND 1 FOLLOWING)",
                "AVG(daily_revenue) GROUP BY DATEADD(day, 7, sale_date)"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "da_02",
            title = "Customer Cohort Retention & Churn Analysis",
            category = "Technical",
            difficulty = "Easy",
            description = "A SaaS subscription company began the month of January with exactly 2,000 active paying subscribers. During January, 100 existing subscribers cancelled their accounts, and the marketing team acquired 300 brand new subscribers, ending January with 2,200 active subscribers.\n\nWhat is the customer churn rate for the month of January, and which formula defines it?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_analyst",
            topic = "Business Analytics",
            exampleInput = "Start of month subscribers = 2,000; Cancellations = 100; New acquisitions = 300",
            exampleOutput = "Monthly customer churn rate percentage and metric definition",
            explanation = "Customer Churn Rate is calculated as (Customers Lost During Period) / (Customers at Start of Period) = 100 / 2,000 = 0.05 = 5.0%. Newly acquired customers are counted in acquisition cohorts and do not alter the starting denominator of the churn formula.",
            constraints = "Standard subscription cohort metric definitions over a monthly billing period.",
            hint = "Customer churn rate evaluates the percentage of customers present at the start of the period who cancelled during that period.",
            options = listOf(
                "15.0%, calculated as (New Customers Acquired) / (Customers at Start of Period) = 300 / 2,000",
                "4.5%, calculated as (Customers Lost During Period) / (Customers at End of Period) = 100 / 2,200",
                "5.0%, calculated as (Customers Lost During Period) / (Customers at Start of Period) = 100 / 2,000",
                "33.3%, calculated as (Customers Lost During Period) / (New Customers Acquired) = 100 / 300"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "da_03",
            title = "Exploratory Data Analysis & Visual Distribution Skew",
            category = "Technical",
            difficulty = "Easy",
            description = "You generate a histogram and summary statistics for a company's customer order value data. You notice that the histogram displays a long tail extending far to the right, and the statistical summary reveals Mean = $145, Median = $62, and Mode = $40.\n\nHow is this distribution classified, and what does the relationship between Mean, Median, and Mode signify?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "data_analyst",
            topic = "Data Visualization",
            exampleInput = "Distribution summary: Mean = 45, Median = 2, Mode = 0 with right tail",
            exampleOutput = "Statistical classification of skewness direction and measure relationships",
            explanation = "A distribution with a long right tail is Positively (Right) Skewed. Infrequent, exceptionally high values pull the arithmetic Mean significantly to the right, while the Median remains resistant and the Mode reflects the most frequent lower price points, establishing Mean > Median > Mode.",
            constraints = "Unimodal continuous financial distribution.",
            hint = "When extreme high values pull the arithmetic average upward to the right, the distribution is right-skewed, causing Mean > Median > Mode.",
            options = listOf(
                "Negatively (Left) Skewed distribution where Mode > Median > Mean",
                "Perfectly Symmetrical Normal Distribution where Mean = Median = Mode",
                "Bimodal Uniform Distribution where variance is zero",
                "Positively (Right) Skewed distribution where Mean > Median > Mode"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "uiux_01",
            title = "WCAG 2.1 AA Contrast Ratio & Touch Sizing",
            category = "Technical",
            difficulty = "Easy",
            description = "You are designing the design system for a mobile application that must comply with the Web Content Accessibility Guidelines (WCAG 2.1 Level AA) and Google Material Design touch ergonomics.\n\nWhat is the minimum required visual color contrast ratio for standard body text (< 18pt regular font), and what is the minimum recommended touch target dimension for interactive components?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "uiux_designer",
            topic = "Accessibility & Inclusive Design",
            exampleInput = "Mobile application UI components evaluated for WCAG 2.1 Level AA compliance",
            exampleOutput = "Standard text contrast threshold and minimum interactive target size in dp",
            explanation = "WCAG 2.1 Level AA mandates a minimum luminance contrast ratio of 4.5:1 against the background for normal text (under 18pt or 14pt bold). For mobile touch accessibility, Material Design 3 and Android standards require interactive touch targets to be at least 48 x 48 dp to accommodate human fingertip interaction.",
            constraints = "WCAG 2.1 Level AA standards and Android Material 3 touch target guidelines.",
            hint = "WCAG AA requires a 4.5:1 contrast ratio for normal text (3:1 for large text >= 18pt), and mobile guidelines specify at least 48x48 dp for reliable tapping.",
            options = listOf(
                "4.5:1 minimum contrast ratio for standard body text, and 48 x 48 dp minimum touch target size",
                "3.0:1 minimum contrast ratio for body text, and 32 x 32 dp minimum touch target size",
                "7.0:1 minimum contrast ratio for body text, and 24 x 24 dp minimum touch target size",
                "2.0:1 minimum contrast ratio for body text, and 64 x 64 dp minimum touch target size"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "uiux_02",
            title = "Information Architecture & Card Sorting Analysis",
            category = "Technical",
            difficulty = "Medium",
            description = "A design team is reorganizing the navigation structure of an enterprise healthcare portal with over 60 distinct features. The team needs to understand how real users naturally group these healthcare services into mental categories before establishing primary menu categories.\n\nWhich research methodology should the team execute, and how do Open versus Closed card sorting differ?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "uiux_designer",
            topic = "Information Architecture",
            exampleInput = "Navigation architecture evaluation: comparison of open vs. closed card sorting",
            exampleOutput = "Methodological differences and appropriate generative research applications",
            explanation = "Card sorting is the standard UX research method for discovering mental models. In an Open Card Sort, participants group feature cards and create their own category names, uncovering user terminology. In a Closed Card Sort, participants place cards into predefined category buckets to validate an established structure.",
            constraints = "Qualitative and quantitative UX information architecture research standards.",
            hint = "Open card sorting is generative (participants create their own category labels), whereas closed card sorting is evaluative (participants sort cards into existing buckets).",
            options = listOf(
                "Closed card sorting lets participants create new categories, while open card sorting uses fixed labels",
                "Open card sorting allows participants to create category labels; closed card sorting sorts into predefined categories",
                "Card sorting is exclusively used to select UI color schemes and font hierarchies",
                "Tree testing is used to create categories, while open card sorting is used to test server latency"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "uiux_03",
            title = "Usability Testing & System Usability Scale (SUS)",
            category = "Technical",
            difficulty = "Easy",
            description = "Following a comprehensive usability test of a redesigned mobile banking app, 25 participants completed the standardized 10-item System Usability Scale (SUS) questionnaire with 5-point Likert scales.\n\nWhat is the industry-standard benchmark SUS score that represents average (50th percentile) usability across software products?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "uiux_designer",
            topic = "UX Research",
            exampleInput = "Standard 10-item SUS questionnaire administered across 30 usability test participants",
            exampleOutput = "Recognized industry benchmark score corresponding to average 50th percentile usability",
            explanation = "Based on empirical research across over 500 usability studies (Brooke, Bangor et al.), the average System Usability Scale (SUS) score is 68. Scores above 68 are considered above average (e.g., 80.3+ is an 'A' grade), while scores below 68 indicate usability deficiencies requiring remediation.",
            constraints = "Standard Brooke/Sauro System Usability Scale methodology on 0-100 scale.",
            hint = "An SUS score of 68 is widely established in UX literature as the average score (equivalent to a 'C' grade).",
            options = listOf(
                "50.0 is the standardized average baseline score on the 0-100 SUS scale",
                "85.0 is the minimum threshold required for a system to be considered usable",
                "68.0 is the recognized industry average benchmark score representing 50th percentile usability",
                "35.0 is the typical average score achieved by consumer mobile applications"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "uiux_04",
            title = "Design Systems & Responsive Modular Grid Scaling",
            category = "Technical",
            difficulty = "Medium",
            description = "When building a scalable UI design system for cross-platform Android and web applications, design systems engineers standardize spacing, padding, margins, and component dimensions using a base 8dp/8px spatial grid.\n\nWhy is the 8dp baseline grid the universal standard in modern UI design systems rather than arbitrary pixel values (such as 7px or 13px)?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "uiux_designer",
            topic = "Design Systems",
            exampleInput = "Design system spatial token architecture using base grid units",
            exampleOutput = "Mathematical and display rendering benefits of 8dp multiples",
            explanation = "Modern displays use varied pixel densities (@1x, @1.5x, @2x, @3x, @4x). Because 8 is an even number divisible by 2 and 4, dimensions using multiples of 8dp scale into clean integer physical pixel counts without creating fractional sub-pixels, preventing visual blur and ensuring cross-platform rendering consistency.",
            constraints = "Cross-platform rendering across varied device pixel densities.",
            hint = "Multiples of 8 divide and scale evenly into whole integers across common device density multipliers (@1x, @2x, @3x, @4x), preventing blurry sub-pixel rendering.",
            options = listOf(
                "Arbitrary odd pixel values like 7px provide superior visual asymmetry on mobile screens",
                "The 8dp grid is mandatory because mobile CPU hardware only computes bytes of 8 bits",
                "CSS and Android layouts do not permit margin values that are not powers of two",
                "Multiples of 8dp scale cleanly into whole integer pixels across common device density factors (@1x to @4x)"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "uiux_05",
            title = "Cognitive Load Reduction & Interaction Micro-States",
            category = "Technical",
            difficulty = "Medium",
            description = "An e-commerce checkout page presents users with 25 distinct form fields and decision options on a single overwhelming screen, resulting in high shopping cart abandonment. The design team refactors the checkout into a clear, 3-step progressive wizard with chunked inputs.\n\nWhich fundamental UX psychological law directly explains why reducing the number of immediate choices decreases user decision time and reduces cognitive friction?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "uiux_designer",
            topic = "Interaction Design",
            exampleInput = "User decision time evaluation under varying counts of interface choices",
            exampleOutput = "Foundational HCI psychological principle governing decision latency",
            explanation = "Hick's Law states that the time it takes for a user to make a decision increases logarithmically with the number and complexity of choices: T = b * log_2(n + 1). Breaking 25 choices into chunked progressive steps lowers the cognitive load n at each step, significantly accelerating decision velocity and conversion.",
            constraints = "Standard human-computer interaction (HCI) cognitive principles.",
            hint = "Hick's Law (Hick-Hyman Law) describes the time it takes to make a decision as a function of the number of possible choices.",
            options = listOf(
                "Hick's Law, which states that decision time increases logarithmically with the number of options presented",
                "Fitts's Law, which states that larger buttons always produce faster checkout decisions",
                "Jakob's Law, which dictates that every checkout flow must look identical to Amazon",
                "Miller's Law, which states that users can effortlessly retain 50 items in working memory"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "cyber_01",
            title = "OWASP Top 10: SQL Injection & XSS Defense",
            category = "Technical",
            difficulty = "Medium",
            description = "During a security audit of a web application, you find the following backend code: query = \"SELECT * FROM accounts WHERE user_id = '\" + request.getParameter(\"id\") + \"'\". An attacker inputs id = ' OR '1'='1' -- to bypass authentication and dump the entire database.\n\nWhich primary defense mechanism completely neutralizes SQL Injection vulnerabilities at the architecture level?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cyber_analyst",
            topic = "Application Security",
            exampleInput = "Untrusted user input passed into database authentication queries",
            exampleOutput = "Definitive secure coding defense against SQL injection attacks",
            explanation = "Parameterized queries (Prepared Statements with bind variables) ensure the database engine compiles the SQL statement structure beforehand and treats all user inputs strictly as literal data rather than executable SQL syntax, completely preventing SQL injection regardless of special characters.",
            constraints = "OWASP Top 10 Application Security Standards (A03:2021-Injection).",
            hint = "Parameterized queries separate the SQL executable code structure from user-provided data parameters.",
            options = listOf(
                "Filtering single quote characters with a simple client-side JavaScript regex replace function",
                "Using Parameterized Prepared Statements with bound parameters so user input is never executed as SQL code",
                "Storing all database credentials in plain text configuration files",
                "Encoding backend SQL error messages into base64 before logging them"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "cyber_02",
            title = "Public Key Infrastructure (PKI) & TLS Handshake",
            category = "Technical",
            difficulty = "Medium",
            description = "When a web browser establishes a secure HTTPS session with a bank web server using TLS 1.3, asymmetric cryptography and symmetric cryptography are both utilized.\n\nWhy does TLS use asymmetric cryptography during the initial handshake, but switch to symmetric encryption (such as AES-GCM) for the actual session data transmission?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cyber_analyst",
            topic = "Cryptography & Network Security",
            exampleInput = "Secure HTTPS connection initialization and session payload transmission",
            exampleOutput = "Roles of asymmetric and symmetric cryptographic schemes during connection lifecycle",
            explanation = "Asymmetric cryptography (certificates and public/private key pairs) authenticates server identity and securely negotiates shared secrets without prior trust. Once negotiated, symmetric encryption (like AES-256-GCM) is used for bulk data transfer because it is several orders of magnitude faster and hardware-accelerated.",
            constraints = "TLS 1.2 / TLS 1.3 protocol standards and cryptographic performance trade-offs.",
            hint = "Asymmetric encryption (public/private keys) is computationally expensive; symmetric encryption is orders of magnitude faster for high-volume streaming data.",
            options = listOf(
                "Symmetric encryption is used for server authentication, while asymmetric encryption encrypts data payloads",
                "Asymmetric encryption is faster than symmetric encryption for large streaming video files",
                "Asymmetric crypto authenticates identity and exchanges keys; symmetric crypto efficiently encrypts session data payloads",
                "Symmetric keys must be transmitted in plain text across the network during every request"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "cyber_03",
            title = "Network Traffic Forensics & Packet Inspection",
            category = "Technical",
            difficulty = "Medium",
            description = "A Security Operations Center (SOC) analyst inspects network firewall logs and observes an external IP address sending 200,000 TCP SYN packets per second to port 443 with spoofed source IPs, but never returning any corresponding ACK packets to complete the TCP three-way handshake.\n\nWhat type of cyber attack is occurring, and what server resource does it exhaust?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cyber_analyst",
            topic = "Network Forensics",
            exampleInput = "Network traffic: Influx of TCP SYN packets with spoofed IPs and no final ACKs",
            exampleOutput = "Attack taxonomy identification and targeted operating system resource exhaustion",
            explanation = "This is a classic TCP SYN Flood Denial of Service (DoS) attack. The attacker exploits the TCP 3-way handshake by sending rapid SYN packets without sending the final ACK, filling the target server's half-open connection backlog table and preventing legitimate clients from connecting.",
            constraints = "TCP/IP state machine dynamics (RFC 793 / RFC 4987).",
            hint = "A SYN flood leaves half-open connections waiting in the server's connection backlog table until memory resources are exhausted.",
            options = listOf(
                "Normal HTTP/2 bidirectional video streaming traffic",
                "Man-in-the-Middle (MitM) ARP cache poisoning attack on the local switch",
                "SQL Injection payload delivery over DNS port 53",
                "TCP SYN Flood Denial of Service (DoS) attack exhausting the server's half-open connection backlog queue"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "cyber_04",
            title = "Role-Based Access Control (RBAC) & OAuth 2.0 Flow",
            category = "Technical",
            difficulty = "Easy",
            description = "A mobile banking app needs to allow users to authorize access to their financial transactions on a third-party budgeting service. Because mobile applications are public clients that cannot securely store a private client_secret, standard Authorization Code Flow is vulnerable to code interception.\n\nWhich OAuth 2.0 extension protocol is mandatory for securing public mobile and Single-Page applications?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cyber_analyst",
            topic = "Identity & Access Management",
            exampleInput = "Public mobile app requesting delegated API access without hardcoded client secrets",
            exampleOutput = "Standard RFC 7636 authorization grant flow for public native clients",
            explanation = "Authorization Code Flow with Proof Key for Code Exchange (PKCE, RFC 7636) prevents authorization code interception on public clients. The client creates a dynamic cryptographic code_verifier and code_challenge, allowing the authorization server to verify the token requester without requiring a static client secret.",
            constraints = "OAuth 2.0 Security Best Current Practice (RFC 7636 / RFC 8252).",
            hint = "PKCE dynamically creates a cryptographic code_verifier and code_challenge for each authorization request.",
            options = listOf(
                "Authorization Code Flow with PKCE (Proof Key for Code Exchange) using dynamic cryptographic challenges",
                "OAuth 2.0 Implicit Grant flow returning access tokens directly in URL hash fragments",
                "Resource Owner Password Credentials Grant sharing the user's raw password directly with third parties",
                "Client Credentials Grant using a hardcoded secret in the mobile application APK"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "cyber_05",
            title = "Incident Response Playbook & SIEM IoC Correlation",
            category = "Technical",
            difficulty = "Hard",
            description = "A Security Information and Event Management (SIEM) system detects confirmed ransomware executing on three enterprise workstations and attempting lateral movement across the internal subnet. The SOC incident response team must immediately follow the standard NIST SP 800-61 incident handling lifecycle.\n\nWhat is the correct sequential order of the NIST Incident Response lifecycle phases?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cyber_analyst",
            topic = "Security Operations",
            exampleInput = "Enterprise security breach requiring structured incident response execution",
            exampleOutput = "Chronological lifecycle phases according to NIST SP 800-61 framework",
            explanation = "NIST SP 800-61 defines the 4 core phases: 1) Preparation, 2) Detection & Analysis, 3) Containment, Eradication & Recovery, and 4) Post-Incident Activity (Lessons Learned). When active malware is detected, immediate Containment prevents lateral spread before Eradication and system Recovery.",
            constraints = "NIST Special Publication 800-61 Rev. 2 Incident Handling Lifecycle.",
            hint = "You must detect the threat, contain it to prevent spreading, eradicate it, recover systems, and conduct post-incident lessons learned.",
            options = listOf(
                "Eradication -> Preparation -> Post-Incident Review -> Detection & Analysis",
                "Preparation -> Detection & Analysis -> Containment, Eradication & Recovery -> Post-Incident Activity",
                "Recovery -> Eradication -> Containment -> Detection -> Preparation",
                "Detection -> System Reformat -> Public Press Release -> Network Rebuild"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "cloud_01",
            title = "Docker Containerization & Multi-Stage Builds",
            category = "Technical",
            difficulty = "Easy",
            description = "You are creating a Dockerfile for a production Java Spring Boot microservice. Compiling the application requires Maven (500 MB) and a full JDK (400 MB), but running the compiled JAR file only requires a minimal Java Runtime Environment (JRE Alpine, 80 MB).\n\nWhich Dockerfile design pattern allows you to build the application with full build tools while keeping the final production container image minimal and secure?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cloud_engineer",
            topic = "Containers & CI/CD",
            exampleInput = "Application build pipeline: Maven compiler + JDK -> Final runnable JAR on Alpine JRE",
            exampleOutput = "Docker build pattern optimizing image size and security posture",
            explanation = "Docker Multi-Stage Builds use multiple FROM statements (e.g. 'FROM maven AS builder' and 'FROM eclipse-temurin:alpine AS runner'). The final stage copies only the compiled JAR artifact via 'COPY --from=builder', leaving compilers, debuggers, and source code out of the production container, drastically reducing image size and attack surface.",
            constraints = "Production OCI container image packaging standards.",
            hint = "Multi-stage builds use multiple FROM instructions in a single Dockerfile, copying only artifacts from the build stage to the final runtime image.",
            options = listOf(
                "Installing all compilers, debuggers, and source code directly into the production container",
                "Running container processes strictly as the root user with host filesystem mounts",
                "Multi-Stage Builds that separate the build environment from the final lean runtime container image",
                "Storing database credentials directly in environment variables committed to public Git repositories"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "cloud_02",
            title = "Kubernetes Pod Auto-Scaling & Load Balancing",
            category = "Technical",
            difficulty = "Medium",
            description = "A microservice deployed on a Kubernetes production cluster experiences sudden traffic surges during flash sales, causing pod CPU utilization to spike from 30% to 90%. The infrastructure team needs the deployment to automatically scale out the number of running pod replicas when average CPU exceeds 70%.\n\nWhich Kubernetes native resource is designed to monitor metrics and automatically adjust the replica count of a Deployment?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cloud_engineer",
            topic = "Cloud Orchestration",
            exampleInput = "Deployment with variable incoming traffic and target CPU utilization threshold",
            exampleOutput = "Native Kubernetes controller responsible for dynamic horizontal pod scaling",
            explanation = "The Horizontal Pod Autoscaler (HPA) continuously monitors resource metrics (such as CPU or Memory utilization via the Metrics Server) and automatically adjusts the replicas field of a Deployment or ReplicaSet to match the target utilization threshold (e.g., 70% CPU).",
            constraints = "Kubernetes core autoscaling API (autoscaling/v2) with metrics-server enabled.",
            hint = "HPA scales out (adds pod replicas) based on observed metric utilization such as CPU or memory.",
            options = listOf(
                "Cluster Autoscaler, which immediately restarts worker nodes when pods experience high traffic",
                "Vertical Pod Autoscaler (VPA), which reduces memory limits during peak traffic periods",
                "Kubernetes Ingress Controller, which rejects all incoming HTTP requests above 100 req/s",
                "Horizontal Pod Autoscaler (HPA), which dynamically scales the number of pod replicas based on CPU/memory metrics"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "cloud_03",
            title = "Linux Administration & Graceful Process Signals",
            category = "Technical",
            difficulty = "Easy",
            description = "When deploying a new version of a web application running as a Linux systemd service or Docker container, the orchestration orchestrator must instruct the active application process to stop accepting new requests, finish processing in-flight database transactions, and terminate cleanly.\n\nWhich POSIX signal should be sent first to initiate a graceful application shutdown, and which signal forcefully kills the process if it fails to exit?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cloud_engineer",
            topic = "Linux Fundamentals",
            exampleInput = "Process termination request with graceful cleanup timeout period",
            exampleOutput = "POSIX signal pair for graceful notification and forceful termination",
            explanation = "SIGTERM (Signal 15) is the standard termination signal that notifies the application to execute cleanup handlers, close network connections, and exit gracefully. If the process does not terminate within a timeout period, SIGKILL (Signal 9) is issued, which is handled directly by the OS kernel and terminates the process immediately.",
            constraints = "Standard POSIX operating system process signals.",
            hint = "SIGTERM (15) can be caught and handled by application cleanup routines. SIGKILL (9) cannot be caught or ignored.",
            options = listOf(
                "Send SIGTERM (Signal 15) for graceful shutdown, followed by SIGKILL (Signal 9) if the process hangs",
                "Send SIGKILL (Signal 9) for graceful cleanup, followed by SIGTERM (Signal 15) for forced termination",
                "Send SIGHUP (Signal 1) to immediately shut down the physical Linux server",
                "Send SIGSTOP (Signal 19) to permanently delete the application's binary from the disk"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "cloud_04",
            title = "VPC Subnetting & High-Availability Network Design",
            category = "Technical",
            difficulty = "Medium",
            description = "You are designing the network architecture for a 3-tier enterprise cloud application in AWS or GCP. The production database cluster contains sensitive financial customer records. The database servers must be able to initiate outbound connections to the Internet to download operating system security updates, but they must NEVER be accessible from the public Internet.\n\nWhich Virtual Private Cloud (VPC) subnet and routing configuration fulfills these security requirements?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cloud_engineer",
            topic = "Cloud Networking",
            exampleInput = "Tier-3 database cluster requiring outbound update access with zero inbound internet ingress",
            exampleOutput = "Secure VPC subnetting and routing architecture satisfying isolation requirements",
            explanation = "The database instances are placed in a Private Subnet without public IPv4 addresses. The private subnet's route table directs default outbound traffic (0.0.0.0/0) to a Network Address Translation (NAT) Gateway residing in a Public Subnet. The NAT Gateway forwards outbound update requests to the Internet without allowing any unsolicited inbound connections.",
            constraints = "Cloud VPC architecture adhering to the Principle of Least Privilege.",
            hint = "Private subnets lack public IP addresses and direct Internet Gateway routes. Outbound traffic is routed through a Network Address Translation (NAT) Gateway in a public subnet.",
            options = listOf(
                "Assign public IP addresses to database nodes and attach an Internet Gateway directly to the private subnet",
                "Place database servers in a Private Subnet and route outbound traffic through a NAT Gateway in a Public Subnet",
                "Place database instances in a Public Subnet with open security group ingress on port 5432",
                "Disable all routing tables and DNS resolution across the entire VPC"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "cloud_05",
            title = "CI/CD Pipeline Automation & Blue-Green Traffic Shifting",
            category = "Technical",
            difficulty = "Medium",
            description = "An enterprise SaaS platform cannot tolerate any downtime during production releases. The engineering team implements a Blue-Green Deployment strategy to deploy version 2.0 while version 1.0 is currently handling live production traffic.\n\nHow does a Blue-Green deployment achieve zero-downtime releases and rapid rollback capability?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = "cloud_engineer",
            topic = "DevOps & CI/CD",
            exampleInput = "Live production traffic on active environment with new release staged in parallel",
            exampleOutput = "Operational mechanism of Blue-Green deployment and instant rollback",
            explanation = "In Blue-Green deployment, two identical production environments exist. Version 1.0 runs in Blue while Version 2.0 is deployed and verified in Green. Once Green passes automated health checks, the load balancer/router switches traffic from Blue to Green instantaneously with zero downtime. If issues arise, traffic is instantly reverted back to Blue.",
            constraints = "Zero-downtime deployment strategy with immediate rollback capability.",
            hint = "Two identical production environments exist. Traffic is switched instantaneously at the load balancer level once the new environment is validated.",
            options = listOf(
                "Taking down the entire production cluster for a scheduled 2-hour maintenance outage",
                "Deploying untested code changes directly to the production database during peak business hours",
                "Deploying to an identical Green environment and switching load balancer traffic instantaneously from Blue to Green",
                "Gradually overwriting server binaries in place without checking application health endpoints"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "apt_01",
            title = "Time and Work: Alternate Work Days",
            category = "Aptitude",
            difficulty = "Medium",
            description = "A can complete a piece of work in 10 days, while B can complete the same work in 15 days. They work on alternate days, with A working on Day 1 and B working on Day 2. They continue this alternating pattern (A on odd days, B on even days) until the entire work is completed.\n\nHow many total days will they take to complete the entire work?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Time and Work",
            exampleInput = "Individual capacities: A = 10 days, B = 15 days. Alternating sequence: Day 1 (A), Day 2 (B)...",
            exampleOutput = "Total number of days required to complete all work units",
            explanation = "Let total work = LCM(10, 15) = 30 units.\nA's 1-day work = 30 / 10 = 3 units/day.\nB's 1-day work = 30 / 15 = 2 units/day.\nIn one 2-day cycle (Day 1 by A + Day 2 by B), work completed = 3 + 2 = 5 units.\nNumber of 2-day cycles required = 30 / 5 = 6 complete cycles.\nTotal days taken = 6 cycles × 2 days/cycle = 12 days.",
            constraints = "Both workers maintain uniform, constant productivity rates throughout.",
            hint = "Find the total work using the LCM of 10 and 15 (30 units). Then determine the work completed in one 2-day cycle (A's daily work + B's daily work).",
            options = listOf(
                "10 days",
                "14 days",
                "15 days",
                "12 days"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "apt_02",
            title = "Probability: Two Cards of the Same Suit",
            category = "Aptitude",
            difficulty = "Easy",
            description = "Two cards are drawn randomly one after another without replacement from a standard, well-shuffled pack of 52 playing cards (containing 4 suits with 13 cards in each suit).\n\nWhat is the exact probability that both drawn cards belong to the same suit?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Probability",
            exampleInput = "Deck: 52 cards, 4 suits of 13 cards each. Sequential draw of 2 cards without replacement.",
            exampleOutput = "Probability of both cards sharing identical suit",
            explanation = "The first card drawn can be any card from any suit (probability = 52/52 = 1). Once the first card is drawn, exactly 12 cards of that same suit remain in the deck out of 51 total remaining cards. Therefore, Probability = 1 × (12 / 51) = 4 / 17 ≈ 23.53%.",
            constraints = "Standard deck composition without jokers. Sampling without replacement.",
            hint = "The first card drawn can be of any suit (probability = 52/52 = 1). For the second card to match that same suit, calculate how many cards of that suit remain out of the remaining total cards.",
            options = listOf(
                "4/17 (~23.5%)",
                "1/4 (25.0%)",
                "1/13 (~7.7%)",
                "1/17 (~5.9%)"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "apt_03",
            title = "Problems on Trains: Crossing in Opposite Directions",
            category = "Aptitude",
            difficulty = "Medium",
            description = "Two trains with lengths of 150 meters and 200 meters are running on parallel railway tracks in opposite directions toward each other. Train 1 travels at a uniform speed of 54 km/h and Train 2 travels at a uniform speed of 72 km/h.\n\nHow many seconds will they take to completely cross each other from the moment their front engines meet?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Time, Speed and Distance",
            exampleInput = "Train lengths: 150 m and 200 m. Speeds: 54 km/h and 72 km/h moving in opposite directions.",
            exampleOutput = "Time in seconds for the two trains to completely pass each other",
            explanation = "Total distance to completely cross each other = Length1 + Length2 = 150 m + 200 m = 350 meters.\nRelative Speed in opposite directions = Speed1 + Speed2 = 54 + 72 = 126 km/h.\nConverting to meters per second: 126 × (5 / 18) = 7 × 5 = 35 m/s.\nTime taken = Total Distance / Relative Speed = 350 / 35 = 10 seconds.",
            constraints = "Constant speeds on parallel tracks. Unit conversion: 1 km/h = 5/18 m/s.",
            hint = "When moving in opposite directions, the relative speed is the sum of both speeds. The total distance to cross is the sum of both train lengths.",
            options = listOf(
                "8 seconds",
                "10 seconds",
                "12 seconds",
                "14 seconds"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "apt_04",
            title = "Profit and Loss: Successive Discounts",
            category = "Aptitude",
            difficulty = "Easy",
            description = "A store manager marks a designer jacket at a Marked Price (MP) of $500. During a festive sale, the store offers two successive discounts of 20% and 10% on the marked price.\n\nWhat is the single equivalent discount percentage, and what is the final selling price paid by a customer?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Profit and Loss",
            exampleInput = "Marked Price = 00 with consecutive discounts of 20% followed by 10%",
            exampleOutput = "Equivalent single discount percentage and final discounted purchase price",
            explanation = "Single Equivalent Discount D = d1 + d2 - (d1 × d2) / 100 = 20 + 10 - (20 × 10) / 100 = 30 - 2 = 28%.\nFinal Selling Price = Marked Price × (1 - D/100) = $500 × (1 - 0.28) = $500 × 0.72 = $360.\n(Alternatively: After 20% discount on $500 = $400; after 10% discount on $400 = $400 - $40 = $360).",
            constraints = "Successive discounts applied sequentially to the reducing balance.",
            hint = "Use the successive discount formula: Equivalent Discount D = d1 + d2 - (d1 × d2) / 100.",
            options = listOf(
                "30% discount, Final Selling Price = $350",
                "25% discount, Final Selling Price = $375",
                "28% discount, Final Selling Price = $360",
                "22% discount, Final Selling Price = $390"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "apt_05",
            title = "Problems on Ages: Father and Son",
            category = "Aptitude",
            difficulty = "Medium",
            description = "The ratio of the present ages of a father and his son is 5 : 2. Exactly 5 years from now, the sum of their ages will be 80 years.\n\nWhat is the present age of the father?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Ages",
            exampleInput = "Current age ratio Father : Son = 5 : 2. Combined age in 5 years = 80 years.",
            exampleOutput = "Present age of the father in years",
            explanation = "Let Father's present age = 5x and Son's present age = 2x.\nIn 5 years, Father's age = 5x + 5, and Son's age = 2x + 5.\nGiven sum in 5 years: (5x + 5) + (2x + 5) = 80\n=> 7x + 10 = 80\n=> 7x = 70\n=> x = 10.\nTherefore, Father's present age = 5x = 5 × 10 = 50 years (and Son's present age is 20 years).",
            constraints = "Both individuals age at the identical rate of 1 year per calendar year.",
            hint = "Let the present ages of father and son be 5x and 2x. In 5 years, their ages will be (5x + 5) and (2x + 5). Set their sum equal to 80.",
            options = listOf(
                "45 years",
                "55 years",
                "60 years",
                "50 years"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "apt_06",
            title = "Pipes and Cisterns: Two Inflow Pipes",
            category = "Aptitude",
            difficulty = "Easy",
            description = "Pipe A can fill an empty water reservoir in 6 hours, while Pipe B can fill the same empty reservoir in 8 hours. If both pipes are opened simultaneously into the empty reservoir, how many hours will they take to fill the reservoir completely?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Pipes and Cisterns",
            exampleInput = "Filling times: Pipe A = 6 hours, Pipe B = 8 hours running simultaneously",
            exampleOutput = "Total elapsed time in hours to fill the cistern completely",
            explanation = "Work done by Pipe A in 1 hour = 1/6 of reservoir.\nWork done by Pipe B in 1 hour = 1/8 of reservoir.\nCombined work done by (A + B) in 1 hour = 1/6 + 1/8 = (4 + 3) / 24 = 7/24 of reservoir.\nTotal time required to fill reservoir completely = 1 / (7/24) = 24/7 hours = 3 3/7 hours (approx. 3 hours 25.7 minutes).",
            constraints = "Uniform flow rates with zero leakage or evaporation.",
            hint = "Calculate the 1-hour filling rate of each pipe (1/6 and 1/8), add them to find the combined 1-hour rate, and take the reciprocal.",
            options = listOf(
                "3 3/7 hours (24/7 hours)",
                "3 hours",
                "4 hours",
                "7 hours"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "apt_07",
            title = "Ratio and Proportion: Mixture Dilution",
            category = "Aptitude",
            difficulty = "Medium",
            description = "A container holds 60 liters of a milk and water mixture where the ratio of milk to water is 2 : 1. A dairy worker wants to dilute the mixture by adding pure water so that the new ratio of milk to water becomes 1 : 2.\n\nHow many liters of pure water must be added to the mixture?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Ratio and Proportion",
            exampleInput = "Initial mixture = 60 L with Milk : Water = 2 : 1. Target ratio Milk : Water = 1 : 2.",
            exampleOutput = "Quantity of pure water in liters added to achieve target ratio",
            explanation = "Initial volume = 60 L with Milk : Water = 2 : 1.\nInitial Milk = (2/3) × 60 = 40 liters.\nInitial Water = (1/3) × 60 = 20 liters.\nLet x liters of water be added to achieve Milk : Water = 1 : 2:\n40 / (20 + x) = 1 / 2\n=> 20 + x = 80\n=> x = 60 liters of water.",
            constraints = "Milk volume remains strictly constant throughout the dilution process.",
            hint = "Calculate the initial quantities of milk (40 L) and water (20 L). Set up the equation: Milk / (Water + x) = 1 / 2.",
            options = listOf(
                "40 liters",
                "60 liters",
                "80 liters",
                "50 liters"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "apt_08",
            title = "Simple Interest: Multiplication of Principal",
            category = "Aptitude",
            difficulty = "Easy",
            description = "A sum of money invested under Simple Interest at a constant annual interest rate doubles itself in exactly 8 years.\n\nIn how many total years will the principal amount become 4 times its original value at the exact same rate of simple interest?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Simple and Compound Interest",
            exampleInput = "Principal doubles (Amount = 2P) in 8 years under constant simple interest",
            exampleOutput = "Total investment tenure in years to quadruple initial principal",
            explanation = "Let the principal be P. When it doubles in 8 years, the Amount A = 2P, so Simple Interest earned SI_1 = 2P - P = P in 8 years.\nTo become 4 times the original principal, Amount A = 4P, so required Simple Interest SI_2 = 4P - P = 3P.\nBecause Simple Interest is directly proportional to time (T), earning 3 times the interest takes 3 × 8 years = 24 years.",
            constraints = "Constant annual interest rate with non-compounding simple interest.",
            hint = "When principal doubles, Simple Interest earned = P in 8 years. To become 4 times the principal (Amount = 4P), interest earned must be 3P.",
            options = listOf(
                "16 years",
                "32 years",
                "24 years",
                "20 years"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "apt_09",
            title = "Averages: Team Batting Average Change",
            category = "Aptitude",
            difficulty = "Easy",
            description = "In a cricket match, the average score of the first 5 batsmen was 50 runs. When the 6th batsman batted and his score was included, the overall average score of all 6 batsmen increased to 55 runs.\n\nHow many runs did the 6th batsman score in the match?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Average",
            exampleInput = "5 batsmen average 50 runs. With 6th batsman, 6-player average becomes 55 runs.",
            exampleOutput = "Individual score of 6th batsman required to produce average increase",
            explanation = "Total runs scored by 5 batsmen = 5 × 50 = 250 runs.\nTotal runs scored by 6 batsmen = 6 × 55 = 330 runs.\nRuns scored by the 6th batsman = Total(6) - Total(5) = 330 - 250 = 80 runs.",
            constraints = "All scores are non-negative integers.",
            hint = "Calculate the total runs scored by 5 batsmen (5 × 50) and the total runs scored by 6 batsmen (6 × 55), then find the difference.",
            options = listOf(
                "75 runs",
                "85 runs",
                "90 runs",
                "80 runs"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "apt_10",
            title = "Percentages: Price Increase & Consumption Adjustment",
            category = "Aptitude",
            difficulty = "Easy",
            description = "Due to global market inflation, the retail price of petrol increases by 25%. A vehicle owner wants to adjust their monthly driving habits so that their total financial expenditure on petrol remains exactly unchanged.\n\nBy what percentage must the owner reduce their petrol consumption to keep expenditure constant?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Percentages",
            exampleInput = "Price increase = 25%. Budget expenditure is held strictly constant.",
            exampleOutput = "Percentage reduction in consumption required to maintain constant spend",
            explanation = "Let initial Price = $100 and Consumption = 100 units (Expenditure = $10,000).\nNew Price = $125. To keep expenditure at $10,000, new Consumption = 10,000 / 125 = 80 units.\nReduction in consumption = 100 - 80 = 20 units = 20%.\nFormula: [r / (100 + r)] × 100% = [25 / (100 + 25)] × 100% = (25 / 125) × 100% = (1/5) × 100% = 20%.",
            constraints = "Expenditure = Price * Consumption = Constant.",
            hint = "Use the standard expenditure conservation formula: Percentage Reduction = [r / (100 + r)] × 100%, where r is the price increase percentage.",
            options = listOf(
                "20% reduction",
                "25% reduction",
                "16.67% reduction",
                "15% reduction"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "iv_01",
            title = "STAR Method: Resolving Technical Disagreements",
            category = "Interview",
            difficulty = "Easy",
            description = "During a behavioral interview for a software engineering position, the interviewer asks: 'Tell me about a time you had a strong technical disagreement with a teammate on an architectural decision. How did you handle it?'\n\nWhich structured communication strategy and behavioral approach demonstrates the highest level of professional maturity and engineering leadership?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Behavioral Interviews",
            exampleInput = "Behavioral interview prompt regarding engineering disagreements and conflict resolution",
            exampleOutput = "Structured behavioral response framework and constructive resolution strategy",
            explanation = "The STAR framework (Situation, Task, Action, Result) provides a clear narrative. A strong answer highlights active listening, depersonalizing the conflict by building prototypes or benchmarking metrics, aligning on customer requirements, and achieving a successful measurable outcome while maintaining team cohesion.",
            constraints = "Standard software engineering behavioral competency interview evaluation.",
            hint = "Structure your response using the STAR method: Situation, Task, collaborative data-driven Action, and positive business Result.",
            options = listOf(
                "Explain why your architecture was objectively superior and how you convinced management to overrule your peer",
                "Use the STAR method: explain the Situation, Task, collaborative data-driven Action (prototyping/benchmarks), and positive Result",
                "State that you always immediately concede and adopt the other person's proposal to avoid workplace conflict",
                "Focus primarily on describing the teammate's lack of technical competence to justify your position"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "iv_02",
            title = "System Design: Mitigating Read-Heavy Database Bottlenecks",
            category = "Interview",
            difficulty = "Medium",
            description = "In a system design interview, you are asked to scale an e-commerce platform. During flash sale events, the platform experiences severe database read latency spikes where 92% of incoming queries are repeated reads of product catalog data, pushing the primary relational database CPU to 95%.\n\nWhich architectural strategy is most appropriate and cost-effective as the initial mitigation to relieve read database pressure?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "System Design",
            exampleInput = "Workload profile: 92% read traffic, 8% write traffic with saturated primary database CPU",
            exampleOutput = "Optimal architectural mitigation for read-heavy database bottlenecks",
            explanation = "For read-heavy workloads (90%+ reads), introducing an in-memory caching layer (e.g. Redis or Memcached) with a cache-aside pattern intercepts the vast majority of repetitive catalog queries in sub-millisecond time. Adding horizontal Read Replicas distributes remaining uncached read traffic, freeing the primary database exclusively for critical transactional writes.",
            constraints = "High read-to-write ratio (92:8). Minimal migration risk and high availability required.",
            hint = "Offload read traffic from the primary database by caching popular product records in an in-memory cache and routing remaining read queries to read replicas.",
            options = listOf(
                "Immediately decompose the entire application into 25 microservices with separate NoSQL databases",
                "Replace the relational database with unindexed flat CSV files stored on local server disks",
                "Implement an in-memory caching layer (Redis) and database Read Replicas to offload repetitive read queries",
                "Disable HTTPS encryption and user authentication headers to reduce database CPU overhead"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "iv_03",
            title = "Engineering Leadership: Communicating Technical Debt",
            category = "Interview",
            difficulty = "Easy",
            description = "During a sprint planning meeting, your engineering team identifies that legacy database query code is causing intermittent checkout timeouts and slowing down feature development. Non-technical product managers want to prioritize new customer-facing features instead of refactoring.\n\nHow should an engineer effectively communicate and justify allocating sprint capacity toward technical debt reduction to business stakeholders?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Stakeholder Management",
            exampleInput = "Legacy codebase causing checkout errors; friction between new features vs. refactoring",
            exampleOutput = "Effective communication approach bridging engineering realities and business KPIs",
            explanation = "Product managers prioritize business outcomes. Rather than arguing about code aesthetics, engineers should frame technical debt in terms of business impact: quantified slowdown in future feature delivery velocity, increased probability of revenue-impacting checkout outages, and customer churn.",
            constraints = "Cross-functional communication between engineering and business/product management.",
            hint = "Translate engineering problems into business risks and KPIs: development velocity, customer churn, and revenue loss.",
            options = listOf(
                "Use low-level compiler jargon and abstract design patterns to convince product managers they cannot understand the issue",
                "Quietly refactor the legacy modules in secret on production branches without sprint visibility",
                "Refuse to work on any new product features until the entire codebase is rewritten from scratch",
                "Translate technical debt into business outcomes: future feature delivery velocity, outage risk, and revenue loss"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "iv_04",
            title = "Site Reliability: Blameless Post-Mortem Principles",
            category = "Interview",
            difficulty = "Medium",
            description = "A junior engineer accidentally deploys an incorrect environment configuration change that brings down the production billing service for 45 minutes before being rolled back. In a high-performing Site Reliability Engineering (SRE) culture, the team conducts a blameless post-mortem.\n\nWhat is the primary objective of conducting a blameless post-mortem following a production incident?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Production Operations",
            exampleInput = "Production outage caused by accidental misconfiguration during manual deployment",
            exampleOutput = "Core objective of blameless post-mortem and systemic resilience culture",
            explanation = "Blameless post-mortems recognize that human error is a symptom of systemic weaknesses in tools, testing, and processes. The primary goal is to identify systemic root causes (e.g., missing pre-flight validation, absence of canary rollouts, or insufficient automated rollback) and build automated guardrails so that a single human mistake cannot cause an outage.",
            constraints = "SRE principles and incident retrospective culture (Google SRE standard).",
            hint = "A blameless post-mortem assumes people act in good faith and focuses on systemic safeguards, automated tests, and deployment guardrails rather than personal blame.",
            options = listOf(
                "Identify systemic root causes, missing automated validation tests, and deployment guardrails to prevent recurrence",
                "Publicly identify and penalize the engineer who triggered the incident to deter future mistakes",
                "Delete all deployment audit logs and server traces to protect the team from executive scrutiny",
                "Restrict all production deployment permissions exclusively to the Chief Executive Officer"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "tech_01",
            title = "Database Indexing: B+ Trees vs Hash Indexing",
            category = "Technical",
            difficulty = "Medium",
            description = "You are optimizing a relational database table orders containing 10 million rows. Applications frequently execute range queries such as: SELECT * FROM orders WHERE order_date BETWEEN '2026-01-01' AND '2026-03-31'.\n\nWhy is a B+ Tree index structure significantly superior to a Hash Index for executing this range scan query?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "DBMS",
            exampleInput = "Range scan query on 10M rows: SELECT * FROM orders WHERE order_date BETWEEN ...",
            exampleOutput = "Algorithmic indexing characteristics enabling efficient range traversals",
            explanation = "B+ Trees store index entries in sorted order and connect leaf nodes with bidirectional linked pointers. For range queries (BETWEEN, >, <), the B+ Tree performs an O(log N) tree traversal to find the range start and then traverses leaf pages sequentially. Hash indexes use hash functions that scatter values randomly into buckets, only supporting exact equality (=) lookups.",
            constraints = "10 million rows in relational database storage engine (InnoDB/PostgreSQL).",
            hint = "B+ Trees maintain keys in sorted order and link leaf pages sequentially, supporting range scans. Hash indexes scatter keys randomly into buckets without ordering.",
            options = listOf(
                "Hash indexes maintain keys in alphabetical sorted order, making them faster for range queries",
                "B+ Trees keep keys sorted and link leaf nodes sequentially, enabling efficient range traversal in O(log N + K) time",
                "B+ Trees have an O(1) worst-case lookup time for every query type",
                "Hash indexes require fewer disk I/O operations than B+ Trees for range scanning queries"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "tech_02",
            title = "REST vs gRPC & Protocol Buffers",
            category = "Technical",
            difficulty = "Medium",
            description = "An engineering team is designing high-throughput, low-latency inter-service Remote Procedure Call (RPC) communication between hundreds of backend microservices. They must choose between traditional REST over HTTP/1.1 with JSON payloads and gRPC over HTTP/2 with Protocol Buffers.\n\nWhich architectural advantages make gRPC with Protocol Buffers significantly faster and more network-efficient than REST with JSON for internal microservices?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Distributed Systems",
            exampleInput = "Internal microservice architecture handling 100,000 inter-process RPC calls per second",
            exampleOutput = "Protobuf serialization and HTTP/2 transport advantages over JSON and HTTP/1.1",
            explanation = "Protocol Buffers serialize structured data into compact binary payloads that are much smaller and drastically faster to serialize/deserialize than text-based JSON. Furthermore, gRPC uses HTTP/2, which provides bidirectional streaming, header compression (HPACK), and multiplexing of concurrent requests over a single TCP connection.",
            constraints = "High-throughput, low-latency microservice architecture.",
            hint = "Protocol Buffers serialize data into compact binary representations rather than human-readable JSON text, and HTTP/2 supports multiplexing multiple requests over a single TCP connection.",
            options = listOf(
                "JSON text parsing is inherently faster and more compact than binary deserialization",
                "REST over HTTP/1.1 provides built-in bidirectional streaming and header compression by default",
                "gRPC uses compact binary Protocol Buffers and HTTP/2 multiplexing, reducing payload size and connection overhead",
                "Protocol Buffers transmit data without any predefined schemas or type contracts"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "tech_03",
            title = "Process Synchronization & Mutex vs Semaphore",
            category = "Technical",
            difficulty = "Medium",
            description = "A multi-threaded server application manages access to a fixed pool of N = 5 database connections shared among 50 worker threads. Threads must acquire a connection before executing queries and release it back to the pool immediately upon completion.\n\nWhat is the key functional difference between a Mutex (Mutual Exclusion lock) and a Counting Semaphore in managing concurrency and resource access?\n\nSelect the correct answer from the four options below.",
            xpReward = 20,
            careerId = null,
            topic = "Operating Systems",
            exampleInput = "Multi-threaded resource pool: 50 threads competing for N = 5 database connections",
            exampleOutput = "Functional distinction between ownership locking and resource counting semantics",
            explanation = "A Mutex is a locking mechanism with thread ownership: only the single thread that locked the mutex can unlock it (strictly 1 thread in critical section). A Counting Semaphore is a signaling mechanism initialized with value N, allowing up to N concurrent threads to acquire permits simultaneously, making it ideal for resource pools.",
            constraints = "Multi-threaded concurrency control and resource pool management.",
            hint = "A Mutex has strict ownership (only the thread that acquired the lock can release it). A Counting Semaphore maintains a counter of available resources and acts as a signaling mechanism.",
            options = listOf(
                "A Semaphore can only ever allow 1 thread to access a resource at all times, identical to a mutex",
                "A Mutex allows any thread in the process to unlock it regardless of which thread acquired it",
                "Counting Semaphores can only be used in single-threaded applications",
                "A Mutex provides exclusive 1-thread locking with ownership; a Counting Semaphore tracks N available resource permits"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "apt_11",
            title = "Number Systems: Modular Remainder Theorem",
            category = "Aptitude",
            difficulty = "Medium",
            description = "When an integer N is divided by 13, the remainder is 7. If the square of the same integer (N^2) is divided by 13, what is the resulting remainder?\n\nSelect the correct remainder from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Number Systems",
            exampleInput = "Divisor d = 13, Rem(N / 13) = 7",
            exampleOutput = "Remainder when N^2 is divided by 13",
            explanation = "According to modular arithmetic properties: Rem(N^2 / d) = Rem([Rem(N / d)]^2 / d). Here, Rem(N / 13) = 7. Thus, 7^2 = 49. Dividing 49 by 13 gives 49 = 3 × 13 + 10, leaving a remainder of 10.",
            constraints = "N is a positive integer. Division by 13.",
            hint = "Apply the remainder rule for exponents: Rem(N^2 / 13) is equal to Rem((7^2) / 13).",
            options = listOf(
                "10",
                "7",
                "4",
                "9"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "apt_12",
            title = "Profit and Loss: Markup and Cash Discount",
            category = "Aptitude",
            difficulty = "Medium",
            description = "A electronics vendor marks their inventory 40% above the cost price (CP) and offers a 15% trade discount on the marked price for direct payments. What is the vendor's net profit percentage on these sales?\n\nSelect the correct percentage from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Profit and Loss",
            exampleInput = "Markup = +40% on CP, Discount = 15% on Marked Price",
            exampleOutput = "Net profit percentage on Cost Price",
            explanation = "Let Cost Price = 100 units. Marked Price (MP) = 100 × 1.40 = 140 units. Discount = 15% of 140 = 0.15 × 140 = 21 units. Selling Price (SP) = 140 - 21 = 119 units. Net Profit = SP - CP = 119 - 100 = 19 units = 19% profit.",
            constraints = "Standard percentage markup and discount calculations.",
            hint = "Assume CP = 100. Calculate Marked Price as 140, then subtract 15% of 140 to obtain the Selling Price.",
            options = listOf(
                "25.0%",
                "19.0%",
                "21.0%",
                "17.5%"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "apt_13",
            title = "Time, Speed and Distance: Stream Current Speed",
            category = "Aptitude",
            difficulty = "Medium",
            description = "A boat navigates 36 km downstream along a river in 3 hours and returns upstream covering 24 km in 4 hours. What is the rate of flow (speed) of the river current?\n\nSelect the correct speed in km/h from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Time, Speed & Distance",
            exampleInput = "Downstream: 36 km in 3 hours; Upstream: 24 km in 4 hours",
            exampleOutput = "River current speed in km/h",
            explanation = "Downstream speed (u + v) = 36 km / 3 h = 12 km/h. Upstream speed (u - v) = 24 km / 4 h = 6 km/h, where u is still-water speed and v is stream speed. Subtracting the two equations gives: 2v = 12 - 6 = 6, so v = 3.0 km/h.",
            constraints = "Constant current speed and uniform boat engine output.",
            hint = "Speed of stream current = (Downstream speed - Upstream speed) / 2.",
            options = listOf(
                "4.5 km/h",
                "2.0 km/h",
                "3.0 km/h",
                "1.5 km/h"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "apt_14",
            title = "Time and Work: Equivalence and Combined Team Rate",
            category = "Aptitude",
            difficulty = "Medium",
            description = "A construction task can be completed in 14 days by either 12 men or 18 women. Working at the same steady rate, how many days will a combined workforce of 8 men and 16 women take to complete the exact same task?\n\nSelect the correct number of days from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Time and Work",
            exampleInput = "Work rate: 12 men = 18 women; Target workforce: 8 men + 16 women",
            exampleOutput = "Total completion time in days",
            explanation = "Since 12 Men = 18 Women, 1 Man = 1.5 Women. Total work = 18 Women × 14 days = 252 woman-days. The combined team has 8 men + 16 women = (8 × 1.5) + 16 = 12 + 16 = 28 women equivalent. Days required = 252 / 28 = 9 days.",
            constraints = "Uniform efficiency across individuals within each group.",
            hint = "Convert the men's work capacity into equivalent women's capacity (1 man = 1.5 women) and calculate total woman-days.",
            options = listOf(
                "12 days",
                "10 days",
                "7 days",
                "9 days"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "apt_15",
            title = "Ratios & Proportions: Partnership Profit Distribution",
            category = "Aptitude",
            difficulty = "Medium",
            description = "Three tech founders A, B, and C invest capital into a cloud venture. A invests ₹40,000 for 12 months, B invests ₹60,000 for 8 months, and C invests ₹80,000 for 6 months. If the annual profit generated is ₹1,05,000, what is B's profit share?\n\nSelect the correct profit amount in Rupees from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Ratios & Proportions",
            exampleInput = "Investments: A (₹40k, 12 mo), B (₹60k, 8 mo), C (₹80k, 6 mo); Total profit = ₹1,05,000",
            exampleOutput = "B's financial profit share in Rupees",
            explanation = "Profit ratio equals the product of Capital × Time. Ratio A : B : C = (40,000 × 12) : (60,000 × 8) : (80,000 × 6) = 480,000 : 480,000 : 480,000 = 1 : 1 : 1. Each partner receives an equal 1/3 share. B's share = 1/3 × ₹1,05,000 = ₹35,000.",
            constraints = "Profits divided strictly in proportion to (capital × duration).",
            hint = "Calculate the effective investment unit for each partner by multiplying capital by duration in months.",
            options = listOf(
                "₹35,000",
                "₹42,000",
                "₹28,000",
                "₹30,000"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "apt_16",
            title = "Averages: Weighted Average Across Batches",
            category = "Aptitude",
            difficulty = "Medium",
            description = "In an engineering college department, Section A has 40 students with an average score of 76 in a data structures exam, while Section B has 60 students with an average score of 84. What is the combined mean score across all 100 students?\n\nSelect the correct mean score from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Averages",
            exampleInput = "Section A: n1 = 40, x1 = 76; Section B: n2 = 60, x2 = 84",
            exampleOutput = "Overall weighted mean score",
            explanation = "Combined average = (Total score A + Total score B) / Total students = ((40 × 76) + (60 × 84)) / (40 + 60) = (3040 + 5040) / 100 = 8080 / 100 = 80.8.",
            constraints = "Weighted arithmetic mean calculation.",
            hint = "Compute total points for both sections first: (40 × 76) + (60 × 84), then divide by 100.",
            options = listOf(
                "80.0",
                "80.8",
                "81.2",
                "79.5"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "apt_17",
            title = "Percentages & Set Theory: Campus Placement Offer Distribution",
            category = "Aptitude",
            difficulty = "Medium",
            description = "In a graduating college batch, 65% of students received job offers in software engineering, 45% received offers in data analytics, and 20% received offers in both domains. What percentage of students received an offer in at least one of these two domains?\n\nSelect the correct percentage from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Percentages",
            exampleInput = "P(Software) = 65%, P(Data) = 45%, P(Both) = 20%",
            exampleOutput = "Percentage of students with at least one domain offer",
            explanation = "Using the Principle of Inclusion-Exclusion: P(A ∪ B) = P(A) + P(B) - P(A ∩ B) = 65% + 45% - 20% = 110% - 20% = 90%. Thus, 90% of students received at least one job offer.",
            constraints = "Standard 2-set inclusion-exclusion percentage formula.",
            hint = "Add the individual domain percentages and subtract the overlapping students who received offers in both domains.",
            options = listOf(
                "85%",
                "75%",
                "90%",
                "95%"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "apt_18",
            title = "Probability: Independent Vulnerability Detection Scanners",
            category = "Aptitude",
            difficulty = "Medium",
            description = "Two independent security tools, Scanner A and Scanner B, inspect a microservice for vulnerabilities. Scanner A has a 0.80 probability of detecting a flaw, while Scanner B has a 0.75 probability. If a flaw exists, what is the probability that it is detected by at least one scanner?\n\nSelect the correct probability from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Probability",
            exampleInput = "P(A) = 0.80, P(B) = 0.75, Independent events",
            exampleOutput = "P(At least one scanner detects the vulnerability)",
            explanation = "The complementary probability that both scanners fail to detect the flaw is: P(A fails) × P(B fails) = (1 - 0.80) × (1 - 0.75) = 0.20 × 0.25 = 0.05. Therefore, P(at least one detects) = 1 - 0.05 = 0.95.",
            constraints = "Mutually independent detection events.",
            hint = "Calculate the complementary probability that both scanners miss the flaw: (1 - 0.80) × (1 - 0.75), then subtract from 1.",
            options = listOf(
                "0.60",
                "0.85",
                "0.90",
                "0.95"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "apt_19",
            title = "Logical Reasoning: Directional Tracking and Displacement",
            category = "Aptitude",
            difficulty = "Medium",
            description = "An autonomous delivery drone leaves a dispatch hub and flies 12 km North. It then executes a 90-degree right turn and travels 9 km East. Finally, it turns right again and flies 12 km South to its delivery destination. What is the drone's net displacement distance and direction from the hub?\n\nSelect the correct displacement from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Logical Reasoning",
            exampleInput = "Leg 1: 12 km North; Leg 2: 9 km East; Leg 3: 12 km South",
            exampleOutput = "Straight-line displacement vector from initial origin",
            explanation = "Starting at origin (0, 0): flying 12 km North reaches (0, 12). Turning right (East) 9 km reaches (9, 12). Turning right (South) 12 km reaches (9, 12 - 12) = (9, 0). The delivery position is located exactly 9 km due East from the dispatch hub.",
            constraints = "Cartesian 2D navigation coordinates.",
            hint = "Track coordinates on a 2D plane: North (+y), East (+x), South (-y). Notice that the 12 km North and 12 km South movements cancel out.",
            options = listOf(
                "9 km East",
                "15 km North-East",
                "12 km East",
                "9 km North"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "apt_20",
            title = "Data Interpretation: Growth Rate of Cloud Infrastructure Cost",
            category = "Aptitude",
            difficulty = "Medium",
            description = "A technology startup reviews its quarterly cloud expenditure reports:\n• Q1 Compute Cost: \$60,000 (out of \$120,000 total)\n• Q2 Compute Cost: \$90,000 (out of \$150,000 total)\n• Q3 Compute Cost: \$108,000 (out of \$180,000 total)\n\nWhat is the percentage increase in Compute expenditure from Q1 to Q3?\n\nSelect the correct percentage increase from the options below.",
            xpReward = 20,
            careerId = null,
            topic = "Data Interpretation",
            exampleInput = "Q1 Compute = \$60,000; Q3 Compute = \$108,000",
            exampleOutput = "Percentage growth rate of Compute spending",
            explanation = "Initial spending in Q1 = \$60,000. Final spending in Q3 = \$108,000. Absolute increase = \$108,000 - \$60,000 = \$48,000. Percentage increase = (Increase / Initial) × 100% = (\$48,000 / \$60,000) × 100% = 0.8 × 100% = 80%.",
            constraints = "Percentage growth calculation based on initial Q1 baseline.",
            hint = "Percentage increase = ((Q3 Value - Q1 Value) / Q1 Value) × 100%. Compute is \$60k in Q1 and \$108k in Q3.",
            options = listOf(
                "50%",
                "80%",
                "66.7%",
                "48%"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "coding_01",
            title = "Two Pointers: Valid Palindrome String Validation",
            category = "Coding",
            difficulty = "Easy",
            description = "Consider the following algorithm for checking whether a string s is a valid palindrome while ignoring non-alphanumeric characters and case:\n\ndef is_palindrome(s: str) -> bool:\n    left, right = 0, len(s) - 1\n    while left < right:\n        while left < right and not s[left].isalnum():\n            left += 1\n        while left < right and not s[right].isalnum():\n            right -= 1\n        if s[left].lower() != s[right].lower():\n            return False\n        left += 1\n        right -= 1\n    return True\n\nWhat is the asymptotic time complexity and auxiliary space complexity of this implementation?",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Strings & Two Pointers",
            exampleInput = "s = 'A man, a plan, a canal: Panama'",
            exampleOutput = "True in O(N) time and O(1) space",
            explanation = "The two pointers left and right move toward each other from opposite ends of the string. Each character is inspected at most twice as pointers advance, resulting in O(N) linear time. Since only two integer pointer variables are maintained without creating auxiliary copies of the string, the auxiliary space is strictly O(1).",
            constraints = "1 <= s.length <= 2 × 10^5, ASCII characters.",
            hint = "The pointers start at opposite ends and meet in the middle in a single pass without allocating extra data structures.",
            options = listOf(
                "O(N) time and O(N) auxiliary space",
                "O(N^2) time and O(1) auxiliary space",
                "O(N) time and O(1) auxiliary space",
                "O(log N) time and O(1) auxiliary space"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "coding_02",
            title = "Loops & Iteration: Enumerate Output Prediction",
            category = "Coding",
            difficulty = "Medium",
            description = "Predict the output of the following Python program that processes a list of integers using enumerate:\n\nnums = [1, 2, 3, 4, 5]\nresult = []\nfor i, x in enumerate(nums):\n    if x % 2 == 0:\n        result.append(x * 2)\n    else:\n        result.append(x + i)\nprint(result)\n\nWhat is printed when this snippet runs?",
            xpReward = 20,
            careerId = "aiml_engineer",
            topic = "Loops & Conditionals",
            exampleInput = "nums = [1, 2, 3, 4, 5]",
            exampleOutput = "[1, 4, 5, 8, 9]",
            explanation = "Trace each iteration:\n• i=0, x=1 (odd): 1 + 0 = 1\n• i=1, x=2 (even): 2 × 2 = 4\n• i=2, x=3 (odd): 3 + 2 = 5\n• i=3, x=4 (even): 4 × 2 = 8\n• i=4, x=5 (odd): 5 + 4 = 9\nThe resulting list is [1, 4, 5, 8, 9].",
            constraints = "Standard Python 3 list iteration with enumerate indexing.",
            hint = "Enumerate provides pairs (index, value) starting at index 0. Check whether each value is even or odd.",
            options = listOf(
                "[1, 4, 3, 8, 5]",
                "[2, 4, 6, 8, 10]",
                "[0, 4, 5, 8, 9]",
                "[1, 4, 5, 8, 9]"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "coding_03",
            title = "Recursion: Call Stack and Base Case Execution",
            category = "Coding",
            difficulty = "Easy",
            description = "Examine the following recursive function definition:\n\ndef mystery(n: int) -> int:\n    if n <= 1:\n        return 1\n    return n + mystery(n - 2)\n\nWhat integer value is returned when calling mystery(7)?\n\nSelect the correct return value from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Recursion",
            exampleInput = "n = 7",
            exampleOutput = "16",
            explanation = "Unrolling the recursive calls:\nmystery(7) = 7 + mystery(5)\nmystery(5) = 5 + mystery(3)\nmystery(3) = 3 + mystery(1)\nmystery(1) = 1 (hits base case n <= 1)\nSumming back up: 3 + 1 = 4; 5 + 4 = 9; 7 + 9 = 16.",
            constraints = "Integer input n with step size 2.",
            hint = "Trace the recursive chain: 7 + 5 + 3 + base_case.",
            options = listOf(
                "16",
                "28",
                "15",
                "12"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "coding_04",
            title = "Sorting Algorithms: Stability and Worst-Case Bounds",
            category = "Coding",
            difficulty = "Medium",
            description = "Which sorting algorithm guarantees a worst-case time complexity of O(N log N) while also maintaining sorting stability (preserving the relative original order of duplicate keys)?\n\nSelect the correct algorithm from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Sorting Algorithms",
            exampleInput = "Array of records with duplicate comparison keys",
            exampleOutput = "Stable O(N log N) sorting algorithm",
            explanation = "MergeSort uses a divide-and-conquer approach that guarantees O(N log N) time complexity across all cases (best, average, and worst). By breaking ties in favor of the element from the left subarray during merge operations, it maintains stability. In contrast, QuickSort has an O(N^2) worst-case time, and HeapSort is not stable.",
            constraints = "Comparison-based sorting algorithm properties.",
            hint = "QuickSort can degrade to O(N^2) in worst case and HeapSort is unstable. Consider the divide-and-conquer algorithm that merges sorted halves.",
            options = listOf(
                "QuickSort",
                "MergeSort",
                "HeapSort",
                "SelectionSort"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "coding_05",
            title = "Arrays & Sliding Window: Maximum Sum Subarray of Size K",
            category = "Coding",
            difficulty = "Medium",
            description = "Given an integer array nums = [2, 1, 5, 1, 3, 2] and a window size k = 3, what is the maximum sum of any contiguous subarray of size k, and what is the optimal time complexity to find it?\n\nSelect the correct maximum sum and complexity from the options below.",
            xpReward = 20,
            careerId = "data_scientist",
            topic = "Sliding Window",
            exampleInput = "nums = [2, 1, 5, 1, 3, 2], k = 3",
            exampleOutput = "Maximum contiguous subarray sum = 9 in O(N) time",
            explanation = "Evaluating all contiguous windows of length 3:\n• Window 1: [2, 1, 5] -> sum = 8\n• Window 2: [1, 5, 1] -> sum = 7\n• Window 3: [5, 1, 3] -> sum = 9\n• Window 4: [1, 3, 2] -> sum = 6\nThe maximum sum is 9. Using a sliding window (subtracting the element leaving the window and adding the incoming element) requires O(1) work per step, yielding optimal O(N) total time.",
            constraints = "Array length N >= k, where k is a positive integer.",
            hint = "Slide a window of size 3 across the array. The window with elements [5, 1, 3] gives the highest sum.",
            options = listOf(
                "Max sum = 8 in O(N log N) time",
                "Max sum = 10 in O(N^2) time",
                "Max sum = 9 in O(N) time",
                "Max sum = 9 in O(N^2) time"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "coding_06",
            title = "Functions & Scope: Mutable Default Arguments Trap",
            category = "Coding",
            difficulty = "Medium",
            description = "Analyze the following Python code snippet containing a function with a default parameter:\n\ndef append_to_list(val, items=[]):\n    items.append(val)\n    return items\n\nlist1 = append_to_list(10)\nlist2 = append_to_list(20)\nprint(list1)\n\nWhat is the exact output printed by print(list1)?",
            xpReward = 20,
            careerId = "aiml_engineer",
            topic = "Functions & Scope",
            exampleInput = "Sequential calls: append_to_list(10), then append_to_list(20)",
            exampleOutput = "[10, 20]",
            explanation = "In Python, default parameter values are evaluated once when the function is defined, not each time it is invoked. The empty list items=[] is created at function definition. Calling append_to_list(10) appends 10 to that shared list. The subsequent call append_to_list(20) mutates the exact same list instance, appending 20. Because list1 references that same underlying list object, print(list1) outputs [10, 20].",
            constraints = "Standard Python execution model for mutable default arguments.",
            hint = "Default argument expressions in Python are evaluated once at def time. Both list1 and list2 reference the identical list instance.",
            options = listOf(
                "[10]",
                "[20]",
                "[10, 10]",
                "[10, 20]"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "coding_07",
            title = "OOP Principles: Information Hiding and Encapsulation",
            category = "Coding",
            difficulty = "Easy",
            description = "In Object-Oriented Programming (OOP), which core principle restricts direct external access to an object's internal state variables and requires callers to interact through well-defined public methods and interfaces?\n\nSelect the correct OOP principle from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Object-Oriented Programming",
            exampleInput = "Private member variables accessed via public getters and setters",
            exampleOutput = "Encapsulation",
            explanation = "Encapsulation bundles data (attributes) and methods that operate on that data into a single unit (a class) while restricting direct outside access to internal implementation details (data hiding). This safeguards internal state from unauthorized or unintended modifications.",
            constraints = "Core OOP design principles (Encapsulation, Polymorphism, Inheritance, Abstraction).",
            hint = "This principle acts as a protective shield around an object's internal variables, bundling data and methods together.",
            options = listOf(
                "Encapsulation",
                "Polymorphism",
                "Inheritance",
                "Dynamic Dispatch"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "coding_08",
            title = "Conditionals: Boolean Operator Short-Circuit Evaluation",
            category = "Coding",
            difficulty = "Easy",
            description = "Consider the following Python function:\n\ndef check_ratio(x: int, y: int) -> bool:\n    return x > 0 and (y / x) > 2\n\nWhat occurs when the function is called with arguments check_ratio(0, 5)?\n\nSelect the correct runtime behavior from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Conditionals & Logic",
            exampleInput = "x = 0, y = 5",
            exampleOutput = "False without raising ZeroDivisionError",
            explanation = "Python's logical 'and' operator uses short-circuit evaluation: it evaluates operands from left to right. When x = 0, the first condition (x > 0) evaluates to False. Because False and anything is always False, the interpreter does not evaluate the right operand (y / x > 2). Thus, no division by zero occurs, and the function safely returns False.",
            constraints = "Standard short-circuit boolean evaluation rules.",
            hint = "If the first condition in an 'and' expression evaluates to False, does Python ever evaluate the second condition?",
            options = listOf(
                "Raises a ZeroDivisionError runtime exception",
                "Returns False due to short-circuit evaluation of the logical AND operator",
                "Returns None without executing the expression",
                "Returns True by default"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "coding_09",
            title = "Searching Algorithms: Binary Search Applicability and Bound",
            category = "Coding",
            difficulty = "Easy",
            description = "A developer needs to search for an integer in an array containing N = 1,000,000 elements. Under what structural precondition is Binary Search applicable, and what is its maximum number of comparisons in the worst case?\n\nSelect the correct prerequisite and bound from the options below.",
            xpReward = 20,
            careerId = "data_analyst",
            topic = "Searching Algorithms",
            exampleInput = "Array size N = 1,000,000",
            exampleOutput = "Requires sorted order; ceil(log2(1,000,000)) = 20 comparisons",
            explanation = "Binary search halves the search space with each comparison, requiring that the array elements are sorted in monotonic order. For N = 1,000,000 elements, the maximum number of comparisons is bounded by ceil(log2(1,000,000)) = ceil(19.93) = 20 comparisons, compared to 1,000,000 in linear search.",
            constraints = "Search over random access array structure.",
            hint = "Binary search relies on ordering to discard half the elements at each step. 2^20 is approximately 1,048,576.",
            options = listOf(
                "Array must be stored in a Hash Table; worst-case 1 comparison",
                "Array can be unsorted; worst-case 1,000,000 comparisons",
                "Array must be sorted; worst-case approximately 20 comparisons",
                "Array must contain unique values; worst-case 500,000 comparisons"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "coding_10",
            title = "Complexity Analysis: Asymptotic Growth of Nested While Loops",
            category = "Coding",
            difficulty = "Medium",
            description = "Analyze the asymptotic time complexity of the following algorithm in Big-O notation:\n\ndef process_matrix(n: int) -> int:\n    count = 0\n    i = 1\n    while i < n:\n        j = 0\n        while j < n:\n            count += 1\n            j += 1\n        i *= 2\n    return count\n\nWhat is the tight worst-case time complexity of process_matrix(n)?",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Time Complexity Analysis",
            exampleInput = "Positive integer n",
            exampleOutput = "O(n log n)",
            explanation = "The outer loop variable i starts at 1 and doubles in each step (i *= 2) until i >= n. Therefore, the outer loop executes ceil(log2(n)) times. For every iteration of the outer loop, the inner loop variable j increments from 0 to n - 1, running exactly n times. Multiplying the loop steps gives O(n × log n).",
            constraints = "Input n is a positive integer.",
            hint = "The outer loop doubles each time (logarithmic), while the inner loop runs n times for every outer step.",
            options = listOf(
                "O(n)",
                "O(n^2)",
                "O(log n)",
                "O(n log n)"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "coding_11",
            title = "Debugging: Array Slicing Boundary and Upper Index Off-by-One",
            category = "Coding",
            difficulty = "Easy",
            description = "A developer writes the following Python code intending to extract a sublist from index 1 through index 4 inclusive from arr:\n\narr = [10, 20, 30, 40, 50, 60]\nsub = arr[1:4]\nprint(sub)\n\nThe program prints [20, 30, 40] instead of [20, 30, 40, 50]. Why did this happen, and what is the proper correction?\n\nSelect the correct root cause and fix from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Debugging & Arrays",
            exampleInput = "arr = [10, 20, 30, 40, 50, 60], desired range: indices 1 through 4",
            exampleOutput = "Python slice stop index is non-inclusive; use arr[1:5]",
            explanation = "In Python slicing syntax arr[start:stop], the stop parameter is exclusive (up to stop - 1). Therefore, arr[1:4] extracts indices 1, 2, and 3. To include index 4 (value 50), the stop parameter must be 5: arr[1:5].",
            constraints = "Standard Python slice notation.",
            hint = "Remember that Python slice ranges are half-open: [start, stop). To include index 4, stop must be 5.",
            options = listOf(
                "The slice stop index is exclusive; it should be written as arr[1:5]",
                "Python list indices start at 1, so the slice should be arr[2:5]",
                "Slicing creates a reversed copy; it should be arr[4:1:-1]",
                "Slices cannot accept integer bounds without using the slice() constructor"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "iv_05",
            title = "Technical Interviews: Navigating Unfamiliar Technologies",
            category = "Interview",
            difficulty = "Easy",
            description = "During a technical interview for a software engineering role, the interviewer asks how you would implement a distributed consensus algorithm that you have never studied or used before. What is the most professionally effective way to respond?\n\nSelect the best approach from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Interview Strategy",
            exampleInput = "Question on unfamiliar advanced technology or framework",
            exampleOutput = "Honest, communicative problem-solving response",
            explanation = "Interviewers value honesty, intellectual curiosity, and transparent problem-solving. Acknowledging unfamiliarity with specific technologies while explaining related fundamentals (e.g., fault tolerance, leader election, network partitions) and reasoning through a solution aloud demonstrates collaborative potential and engineering maturity.",
            constraints = "Standard engineering behavioral and technical interview evaluation.",
            hint = "Be transparent about what you don't know, connect it to related concepts you do know, and think out loud.",
            options = listOf(
                "Pretend you know the algorithm and attempt to guess technical jargon hoping the interviewer will not notice",
                "Transparently acknowledge that you haven't worked with that specific algorithm, explain your conceptual understanding of distributed consistency, and reason through a high-level approach aloud",
                "Immediately state that the topic is outside your syllabus and ask the interviewer to switch to an easier question",
                "Remain silent for several minutes until the interviewer provides the full solution for you"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "iv_06",
            title = "Project Presentation: Articulating Architecture and Impact",
            category = "Interview",
            difficulty = "Medium",
            description = "When an interviewer asks: 'Walk me through the most significant machine learning project listed on your resume,' how should you structure your explanation to make the strongest professional impression?\n\nSelect the most effective structure from the options below.",
            xpReward = 20,
            careerId = "aiml_engineer",
            topic = "Project Presentation",
            exampleInput = "Resume deep-dive on a technical project",
            exampleOutput = "Structured problem-solution-impact narrative",
            explanation = "Effective technical communication starts with business context and user problem, moves to technical architecture and trade-offs (why specific models or tools were chosen over alternatives), and concludes with quantifiable results (e.g., accuracy, latency reduction, user adoption) and retrospective learnings.",
            constraints = "Technical project walkthrough in engineering interviews.",
            hint = "Start with the 'why' (problem and user impact), explain the architectural choices and trade-offs, and finish with measurable outcomes.",
            options = listOf(
                "Recite only the exact lines of Python code and hyperparameters without explaining the underlying problem",
                "Explain the project entirely by complaining about how difficult your project teammates were to work with",
                "State the problem and user impact first, outline the architecture and modeling trade-offs made, and highlight quantifiable results with key learnings",
                "Claim you built every single component including the deep learning framework from scratch by yourself"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "iv_07",
            title = "Teamwork: Constructive Response to Critical Code Review",
            category = "Interview",
            difficulty = "Easy",
            description = "You submit a pull request (PR) for a feature. A senior engineer leaves several critical comments suggesting that your implementation be refactored for better modularity and testability. What is the most constructive way to respond?\n\nSelect the best professional action from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Workplace Communication",
            exampleInput = "Critical code review feedback requesting architectural refactoring",
            exampleOutput = "Collaborative, objective code review engagement",
            explanation = "Code reviews are designed to uphold codebase quality and share knowledge, not evaluate personal worth. The professional response is to evaluate the suggestions objectively, ask clarifying questions to understand the underlying principles, implement the agreed improvements, and view the exchange as a mentorship opportunity.",
            constraints = "Engineering peer code review etiquette.",
            hint = "View code reviews as a collaborative learning opportunity to improve code quality, not as a personal critique.",
            options = listOf(
                "Reject all review comments, argue that your code already compiles, and merge the branch immediately",
                "Complain publicly in the general company chat that the senior reviewer is gatekeeping your contribution",
                "Privately ask another teammate to approve the PR behind the senior engineer's back",
                "Review the feedback objectively, ask clarifying questions on specific architectural suggestions, and implement the improvements collaboratively"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "iv_08",
            title = "Resume Best Practices: Honest Representation of Technical Skills",
            category = "Interview",
            difficulty = "Easy",
            description = "A 2nd-year computer science student completed a 1-day introductory tutorial on Docker. How should this student accurately and professionally represent their containerization skills on their resume for internship applications?\n\nSelect the most appropriate approach from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Resume Best Practices",
            exampleInput = "Limited beginner experience with a tool or technology",
            exampleOutput = "Accurate, ethical skill framing without exaggeration",
            explanation = "Misrepresenting depth of knowledge on a resume often leads to immediate disqualification when technical interviewers ask probing questions. Listing beginner tools accurately under 'Familiar with' or omitting them until practical project experience is gained demonstrates professional integrity and credibility.",
            constraints = "Resume ethics and technical interview readiness.",
            hint = "Never claim 'expert' status on tools you have only used once in a tutorial; list basic familiarity honestly.",
            options = listOf(
                "List 'Basic familiarity with Docker container basics' under tools, or omit it until building a project with it, rather than claiming 'Expert in Docker & Kubernetes orchestration'",
                "List 'Principal Docker Solutions Architect' to ensure applicant tracking systems (ATS) rank the resume highest",
                "List Docker as their primary core programming language alongside Python and Java",
                "Copy the entire Docker official documentation table of contents into the skills section"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "iv_09",
            title = "Analytical Problem Solving: Deconstructing Vague Metrics",
            category = "Interview",
            difficulty = "Medium",
            description = "In an interview for a data analyst or engineering role, the interviewer presents a scenario: 'Our active user engagement dropped 15% over the past week. How would you investigate this?' What is the most structured initial step?\n\nSelect the best analytical strategy from the options below.",
            xpReward = 20,
            careerId = "data_analyst",
            topic = "Problem Solving",
            exampleInput = "Sudden 15% drop in key user engagement metric",
            exampleOutput = "Structured hypothesis-driven segmentation",
            explanation = "Top analysts do not jump to hasty conclusions. A structured response begins by clarifying the definition of the metric, breaking it down across dimensions (device type, operating system, geographical region, new vs. returning users), checking for data pipeline tracking anomalies, and cross-referencing with recent deployments or marketing changes.",
            constraints = "Root cause analysis methodology.",
            hint = "Break the macro metric down into segments (devices, regions, recent software releases) and formulate testable hypotheses.",
            options = listOf(
                "Immediately declare that the application needs a complete redesign without analyzing any data",
                "Ask clarifying questions to segment the metric (e.g., platforms, geographies, recent deployments, seasonal trends) and hypothesize potential root causes before diving into data",
                "Blame the marketing department and wait for the interviewer to change the question",
                "Guess a random number and assume a network outage was responsible"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "iv_10",
            title = "HR Interviews: Answering Long-Term Career Goals",
            category = "Interview",
            difficulty = "Easy",
            description = "During an HR round for a campus placement role, the recruiter asks: 'Where do you see yourself professionally in 3 to 5 years?' What is the most compelling and balanced response strategy?\n\nSelect the most effective answer strategy from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "HR Interview",
            exampleInput = "Classic HR interview prompt regarding future career trajectory",
            exampleOutput = "Goal-oriented professional development trajectory",
            explanation = "Recruiters look for candidates who demonstrate ambition, dedication to learning, and alignment with the organization's growth. Highlighting technical mastery, taking increasing ownership of end-to-end features, and contributing to team mentorship strikes the right balance between personal ambition and organizational value.",
            constraints = "Standard college placement behavioral interview.",
            hint = "Focus on skill mastery, increasing architectural responsibility, and growing into mentorship within the team.",
            options = listOf(
                "State that you plan to leave this company within six months to launch an unrelated cryptocurrency hedge fund",
                "State that you have no plans or ambitions and will simply do whatever task anyone gives you",
                "Articulate clear growth goals: mastering core software engineering practices, taking ownership of production services, and mentoring junior engineers within the company's domain",
                "Demand a guaranteed executive promotion to Chief Technology Officer within the next two years"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "iv_11",
            title = "Engineering Ethics: Deadlines vs Software Quality & Safety",
            category = "Interview",
            difficulty = "Medium",
            description = "Your team faces an imminent deadline to deploy a critical production patch. Skipping automated regression tests would allow deployment 2 hours ahead of the deadline, but risks introducing runtime bugs. What is the standard professional engineering approach?\n\nSelect the most responsible engineering action from the options below.",
            xpReward = 20,
            careerId = "cloud_engineer",
            topic = "Professional Engineering",
            exampleInput = "Time crunch between meeting a release deadline and ensuring automated testing coverage",
            exampleOutput = "Risk communication and uncompromising quality gates",
            explanation = "Professional software engineers prioritize production stability and data integrity over artificial time cuts. Deploying untested code into production can cause severe customer-facing outages. The professional path is to communicate risks transparently to engineering leads, execute critical tests, and maintain safety standards.",
            constraints = "Production reliability standards and release engineering.",
            hint = "Production outages cost far more than short deployment delays. Communicate transparently and never deploy untested critical code.",
            options = listOf(
                "Deploy untested code directly to production and log off for the weekend",
                "Disable all system logging and alert monitors so that failure notifications do not trigger",
                "Blame the QA team for discovering the bug too close to the deployment window",
                "Communicate the risk transparently to your tech lead, write essential regression tests, and ensure deployment safety before releasing"
            ),
            correctIndex = 3
        ),
        PracticeProblem(
            id = "iv_12",
            title = "Placement Interviews: Structuring 'Tell Me About Yourself'",
            category = "Interview",
            difficulty = "Easy",
            description = "At the beginning of an on-campus placement interview, the interviewer asks: 'Tell me about yourself.' Which narrative framework produces the most professional and memorable introduction?\n\nSelect the most effective structure from the options below.",
            xpReward = 20,
            careerId = "aiml_engineer",
            topic = "HR Interview",
            exampleInput = "Opening self-introduction prompt in placement interviews",
            exampleOutput = "Present-Past-Future narrative elevator pitch",
            explanation = "The 'Present-Past-Future' framework provides a crisp 90-to-120-second summary: where you are now (degree, key academic focus), what you have achieved (notable technical projects, internships, problem-solving experience), and why you are here (how your skills align with this specific role and organization).",
            constraints = "Standard introductory interview pitch.",
            hint = "Use the Present-Past-Future model: current degree focus, past project highlights, and future alignment with this role.",
            options = listOf(
                "Present a structured 2-minute journey: your academic focus (Present), key technical projects and internships (Past), and why this role and company excite you (Future)",
                "Recite your high school mark sheets and primary school extracurricular trophies in chronological order",
                "State only your name and roll number and wait for the interviewer to ask specific questions",
                "Read your printed resume line by line from top to bottom with your head down"
            ),
            correctIndex = 0
        ),
        PracticeProblem(
            id = "iv_13",
            title = "Internship Success: Proactive Onboarding and Ramp-Up",
            category = "Interview",
            difficulty = "Easy",
            description = "During the first week of your software engineering internship, you are provided with your development laptop and repository access. What proactive approach best accelerates your ramp-up and leaves a positive impression on your team?\n\nSelect the best onboarding strategy from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Internship Success",
            exampleInput = "First week of an engineering internship",
            exampleOutput = "Proactive documentation reading, local setup, and thoughtful questions",
            explanation = "High-performing interns demonstrate initiative: they thoroughly read setup guides and architectural documentation, set up their local build environment, fix outdated steps in team onboarding docs, and bundle thoughtful, clarifying questions for scheduled 1-on-1 meetings with mentors.",
            constraints = "Professional workplace habits for interns.",
            hint = "Read team documentation, get the local environment running, document any setup hurdles, and ask consolidated questions.",
            options = listOf(
                "Wait silently at your desk until your manager walks over to give you step-by-step instructions",
                "Read the team's onboarding docs and architecture guides, set up the local dev environment, document any gaps you find in the wiki, and prepare targeted questions for your 1-on-1 check-in",
                "Complain to team members that their tech stack is obsolete compared to academic toy projects",
                "Send pull requests to change variable naming conventions in core legacy production services without consulting anyone"
            ),
            correctIndex = 1
        ),
        PracticeProblem(
            id = "iv_14",
            title = "Behavioral Questions: Addressing Past Mistakes and Failures",
            category = "Interview",
            difficulty = "Medium",
            description = "In a behavioral interview round, the interviewer asks: 'Tell me about a time you made a significant mistake or experienced failure on a technical project.' What makes a candidate's response authentic and compelling?\n\nSelect the most effective response from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Behavioral Interview",
            exampleInput = "Interview prompt asking for a genuine past failure or mistake",
            exampleOutput = "Ownership, remediation, and growth mindset",
            explanation = "Interviewers ask about failure to gauge humility, self-awareness, and resilience. Effective candidates describe a genuine technical or collaborative misstep, take personal accountability without blaming others, detail how they resolved the immediate issue, and explain the concrete preventive systems or processes they put in place afterward.",
            constraints = "Behavioral competency evaluation.",
            hint = "Take full personal ownership of a real mistake, focus on how you fixed it, and explain what you learned.",
            options = listOf(
                "Claim that you have never failed at anything because you are a perfectionist",
                "Describe a catastrophic failure and blame your professor or team partner for the entire outcome",
                "Share an authentic mistake, take personal accountability without making excuses, describe the corrective actions you took, and explain what you learned to prevent it from happening again",
                "Refuse to answer the question because discussing mistakes weakens your candidacy"
            ),
            correctIndex = 2
        ),
        PracticeProblem(
            id = "iv_15",
            title = "Workplace Etiquette: Formal Offer Evaluation and Communication",
            category = "Interview",
            difficulty = "Easy",
            description = "You receive a job offer through your college's campus placement drive. What is the most professional conduct when evaluating and communicating your decision regarding the offer?\n\nSelect the most appropriate conduct from the options below.",
            xpReward = 20,
            careerId = "software_dev",
            topic = "Workplace Etiquette",
            exampleInput = "Receiving a campus placement offer letter",
            exampleOutput = "Gratitude, careful contract review, and timely formal communication",
            explanation = "Professional conduct entails expressing timely appreciation, reviewing compensation and joining agreements thoroughly, seeking guidance from the college placement cell on any policy questions, and communicating a formal acceptance or decline within the specified deadline.",
            constraints = "Campus placement policy and professional correspondence.",
            hint = "Express genuine gratitude, review the terms carefully, coordinate with your placement cell, and respond within the deadline.",
            options = listOf(
                "Threaten to reject the offer immediately on public social media unless given an executive bonus",
                "Sign the offer letter and simultaneously sign offers from three other competing on-campus companies violating campus placement rules",
                "Decline to communicate with the recruiter for three weeks hoping they will increase the offer",
                "Express genuine gratitude, review the written offer terms and benefits thoroughly, clarify any joining details with the campus placement cell, and accept or decline formally within the stipulated deadline"
            ),
            correctIndex = 3
        )
    )

    // === QUIZ QUESTIONS BY CAREER ===
    val QUIZ_QUESTIONS_MAP: Map<String, List<QuizQuestion>> = mapOf(
        // General placement speed quiz
        "general" to listOf(
            QuizQuestion(
                id = "q_gen_1",
                question = "Which data structure provides average O(1) time complexity for search, insertion, and deletion?",
                options = listOf("Hash Table", "Binary Search Tree", "Dynamic Array", "Linked List"),
                correctIndex = 0,
                explanation = "Hash Tables map keys to bucket indices using a hash function, providing average O(1) time complexity for lookup, insert, and delete operations.",
                careerId = null
            ),
            QuizQuestion(
                id = "q_gen_2",
                question = "What does ACID stand for in Database Transaction Management?",
                options = listOf("Accuracy, Completeness, Integrity, Dependability", "Atomicity, Consistency, Isolation, Durability", "Asynchronous, Concurrent, Immutable, Distributed", "Authentication, Confidentiality, Identity, Decryption"),
                correctIndex = 1,
                explanation = "ACID properties ensure database transactions are processed reliably: Atomicity (all or nothing), Consistency (valid states), Isolation (concurrent executions don't clash), Durability (persisted).",
                careerId = null
            ),
            QuizQuestion(
                id = "q_gen_3",
                question = "In SQL, which clause filters aggregated grouped records produced by a GROUP BY clause?",
                options = listOf("WHERE", "ORDER BY", "HAVING", "QUALIFY"),
                correctIndex = 2,
                explanation = "The HAVING clause filters aggregated groups after GROUP BY, whereas WHERE filters individual rows prior to grouping.",
                careerId = null
            ),
            QuizQuestion(
                id = "q_gen_4",
                question = "Which sorting algorithm guarantees O(N log N) worst-case time complexity?",
                options = listOf("Quick Sort", "Bubble Sort", "Insertion Sort", "Merge Sort"),
                correctIndex = 3,
                explanation = "Merge Sort always splits the dataset evenly and merges sorted halves in linear time, guaranteeing O(N log N) time complexity in worst, average, and best cases.",
                careerId = null
            ),
            QuizQuestion(
                id = "q_gen_5",
                question = "If 6 men complete a project in 10 days, how many days will 15 men take to complete the same project working at identical efficiency?",
                options = listOf("4 days", "5 days", "6 days", "8 days"),
                correctIndex = 0,
                explanation = "Total work = 6 men × 10 days = 60 man-days. For 15 men: Days = 60 / 15 = 4 days.",
                careerId = null
            )
        ),

        // AI / ML Engineer Quiz
        "aiml_engineer" to listOf(
            QuizQuestion(
                id = "q_ai_1",
                question = "What is the primary role of the Backpropagation algorithm in Deep Learning?",
                options = listOf("Generating synthetic data batches", "Calculating gradients of the loss function with respect to model weights", "Compressing model parameters for mobile devices", "Normalizing dataset inputs to zero mean"),
                correctIndex = 1,
                explanation = "Backpropagation applies the calculus chain rule recursively to compute loss gradients for each trainable weight in the network.",
                careerId = "aiml_engineer"
            ),
            QuizQuestion(
                id = "q_ai_2",
                question = "Which regularization technique randomly deactivates neurons during neural network training to prevent co-adaptation?",
                options = listOf("Batch Normalization", "Early Stopping", "Dropout", "L1 Lasso Regularization"),
                correctIndex = 2,
                explanation = "Dropout randomly disables a fraction of hidden units during forward/backward passes, forcing the network to learn robust, generalized representations.",
                careerId = "aiml_engineer"
            ),
            QuizQuestion(
                id = "q_ai_3",
                question = "In Natural Language Processing, what core mechanism powers Transformer architectures (like BERT and GPT)?",
                options = listOf("Recurrent Hidden States", "Convolutional Max-Pooling", "Markov Decision Chains", "Multi-Head Self-Attention"),
                correctIndex = 3,
                explanation = "Self-attention computes pairwise token compatibility scores across the entire sequence simultaneously, overcoming sequential bottlenecking in RNNs.",
                careerId = "aiml_engineer"
            ),
            QuizQuestion(
                id = "q_ai_4",
                question = "What metric is most appropriate for evaluating a classification model on an extremely imbalanced dataset (e.g., 99% negative, 1% positive)?",
                options = listOf("F1-Score / Precision-Recall AUC", "Classification Accuracy", "Mean Squared Error", "R-Squared"),
                correctIndex = 0,
                explanation = "When classes are heavily skewed, raw accuracy is deceptive. Precision, Recall, and F1-Score evaluate true positive recovery without majority class bias.",
                careerId = "aiml_engineer"
            ),
            QuizQuestion(
                id = "q_ai_5",
                question = "What is the key difference between Overfitting and Underfitting?",
                options = listOf("Overfitting has high bias; Underfitting has high variance", "Overfitting performs well on training data but poorly on test data; Underfitting performs poorly on both", "Overfitting only occurs in unsupervised models; Underfitting only occurs in neural networks", "Overfitting indicates data leakage; Underfitting indicates perfect convergence"),
                correctIndex = 1,
                explanation = "Overfitting memorizes noise in the training set (high variance), leading to poor generalization. Underfitting fails to capture underlying patterns (high bias).",
                careerId = "aiml_engineer"
            )
        ),

        // Software Developer Quiz
        "software_dev" to listOf(
            QuizQuestion(
                id = "q_sd_1",
                question = "In Object-Oriented Programming, which SOLID principle states that classes should be open for extension but closed for modification?",
                options = listOf("Single Responsibility Principle", "Liskov Substitution Principle", "Open/Closed Principle", "Interface Segregation Principle"),
                correctIndex = 2,
                explanation = "The Open/Closed Principle (OCP) advocates designing software entities such that new behavior can be added via inheritance or composition without altering existing source code.",
                careerId = "software_dev"
            ),
            QuizQuestion(
                id = "q_sd_2",
                question = "What is the space complexity of a recursive Depth-First Search on a balanced binary tree of N nodes?",
                options = listOf("O(1)", "O(N)", "O(N log N)", "O(log N)"),
                correctIndex = 3,
                explanation = "In a balanced binary tree, the maximum recursion call stack depth corresponds to tree height, which is O(log N).",
                careerId = "software_dev"
            ),
            QuizQuestion(
                id = "q_sd_3",
                question = "Which HTTP method is defined by RFC specifications as idempotent?",
                options = listOf("PUT", "POST", "PATCH (without precondition)", "CONNECT"),
                correctIndex = 0,
                explanation = "PUT is idempotent: multiple identical requests will produce the exact same server resource state as a single request.",
                careerId = "software_dev"
            ),
            QuizQuestion(
                id = "q_sd_4",
                question = "In modern database concurrency, what phenomenon occurs when a transaction reads data that was modified by an uncommitted concurrent transaction?",
                options = listOf("Phantom Read", "Dirty Read", "Non-repeatable Read", "Deadlock"),
                correctIndex = 1,
                explanation = "A Dirty Read happens when Transaction A reads changes written by Transaction B before Transaction B commits; if B rolls back, A holds invalid data.",
                careerId = "software_dev"
            ),
            QuizQuestion(
                id = "q_sd_5",
                question = "In Kotlin / Java, what is the primary benefit of immutability in multi-threaded environments?",
                options = listOf("Guarantees O(1) memory consumption", "Automatically executes garbage collection immediately", "Eliminates the need for locks and prevents race conditions", "Converts all methods into asynchronous coroutines"),
                correctIndex = 2,
                explanation = "Immutable objects cannot be modified after construction, making them inherently thread-safe without synchronization overhead.",
                careerId = "software_dev"
            )
        ),

        // Data Scientist Quiz
        "data_scientist" to listOf(
            QuizQuestion(
                id = "q_ds_1",
                question = "What is the primary difference between L1 (Lasso) and L2 (Ridge) regression regularization?",
                options = listOf("L1 is only for classification; L2 is only for regression", "L1 squares the error; L2 takes absolute value of error", "L1 requires unnormalized data; L2 requires normalized data", "L1 produces sparse models with feature selection; L2 shrinks coefficients smoothly without eliminating features"),
                correctIndex = 3,
                explanation = "L1 regularization adds a penalty proportional to the absolute values of weights, driving non-informative coefficients to exactly zero (feature selection). L2 penalizes squared weights, shrinking them toward zero without eliminating features.",
                careerId = "data_scientist"
            ),
            QuizQuestion(
                id = "q_ds_2",
                question = "In statistical hypothesis testing, what is a Type I error?",
                options = listOf("Rejecting a true null hypothesis (False Positive)", "Failing to reject a false null hypothesis (False Negative)", "Selecting an inadequate sample size", "Calculating an incorrect standard deviation"),
                correctIndex = 0,
                explanation = "A Type I error occurs when researchers reject the null hypothesis when it is actually true (false positive), with probability equal to the significance level alpha.",
                careerId = "data_scientist"
            ),
            QuizQuestion(
                id = "q_ds_3",
                question = "Which metric measures the purity of splits in Decision Tree algorithms such as CART?",
                options = listOf("Root Mean Square Error", "Gini Impurity", "Cosine Similarity", "Silhouette Coefficient"),
                correctIndex = 1,
                explanation = "CART uses Gini Impurity (or Information Gain via Entropy) to determine the optimal feature split by quantifying class label mixing at each node.",
                careerId = "data_scientist"
            ),
            QuizQuestion(
                id = "q_ds_4",
                question = "What does the Silhouette Coefficient measure in unsupervised clustering evaluation?",
                options = listOf("Model inference execution latency", "The total number of training data points", "How similar an object is to its own cluster compared to neighboring clusters", "The proportion of explained variance in linear projection"),
                correctIndex = 2,
                explanation = "The Silhouette Coefficient ranges from -1 to +1, measuring intra-cluster cohesion versus nearest-cluster separation; higher values indicate well-separated, dense clusters.",
                careerId = "data_scientist"
            ),
            QuizQuestion(
                id = "q_ds_5",
                question = "What technique is used to combat the curse of dimensionality by projecting data onto orthogonal axes of maximum variance?",
                options = listOf("K-Nearest Neighbors", "Stratified K-Fold Cross Validation", "Markov Chain Monte Carlo", "Principal Component Analysis (PCA)"),
                correctIndex = 3,
                explanation = "Principal Component Analysis (PCA) performs eigendecomposition of the covariance matrix to project high-dimensional data onto orthogonal principal components maximizing explained variance.",
                careerId = "data_scientist"
            )
        ),

        // Data Analyst Quiz
        "data_analyst" to listOf(
            QuizQuestion(
                id = "q_da_1",
                question = "In SQL, what is the key difference between UNION and UNION ALL?",
                options = listOf("UNION removes duplicate rows; UNION ALL retains all rows including duplicates", "UNION sorts columns; UNION ALL sorts rows", "UNION works across multiple databases; UNION ALL works within one table", "UNION requires identical column names; UNION ALL allows mismatched types"),
                correctIndex = 0,
                explanation = "UNION performs a distinct deduplication pass across combined results, whereas UNION ALL directly concatenates datasets without removing duplicates, making it faster.",
                careerId = "data_analyst"
            ),
            QuizQuestion(
                id = "q_da_2",
                question = "Which statistical measure is LEAST sensitive to extreme outliers in a skewed numerical dataset?",
                options = listOf("Mean", "Median", "Standard Deviation", "Range"),
                correctIndex = 1,
                explanation = "The Median represents the 50th percentile rank and is robust to extreme values, unlike the Arithmetic Mean which shifts heavily with outliers.",
                careerId = "data_analyst"
            ),
            QuizQuestion(
                id = "q_da_3",
                question = "In data visualization, which chart is most effective for displaying proportions of a whole across 3-5 categories?",
                options = listOf("Scatter Plot", "Box and Whisker Plot", "Donut / Stacked Bar Chart", "Line Chart"),
                correctIndex = 2,
                explanation = "Donut or Stacked Bar charts clearly show part-to-whole categorical compositions when the number of slices is small.",
                careerId = "data_analyst"
            ),
            QuizQuestion(
                id = "q_da_4",
                question = "What type of JOIN returns all records from the left table and matched records from the right table, filling with NULL for unmatched rows?",
                options = listOf("INNER JOIN", "CROSS JOIN", "FULL OUTER JOIN", "LEFT OUTER JOIN"),
                correctIndex = 3,
                explanation = "LEFT OUTER JOIN preserves every row from the primary (left) table regardless of whether matching keys exist in the secondary (right) table.",
                careerId = "data_analyst"
            ),
            QuizQuestion(
                id = "q_da_5",
                question = "What does a Pearson correlation coefficient (r) of -0.85 indicate between two business variables?",
                options = listOf("Strong negative linear relationship", "No linear relationship", "Weak positive relationship", "Definitive causal proof that one variable causes the other"),
                correctIndex = 0,
                explanation = "An r value of -0.85 indicates a strong inverse relationship where increases in one variable consistently correspond to decreases in the other.",
                careerId = "data_analyst"
            )
        ),

        // UI/UX Designer Quiz
        "uiux_designer" to listOf(
            QuizQuestion(
                id = "q_ux_1",
                question = "According to Fitts's Law in user interface design, the time required to rapidly move to a target area is a function of what two factors?",
                options = listOf("Target color and background contrast", "Target distance and target width/size", "Font size and line spacing", "Screen refresh rate and mouse DPI"),
                correctIndex = 1,
                explanation = "Fitts's Law states that target acquisition speed depends on distance to the target and target size. Larger, closer interactive buttons are faster and easier to tap.",
                careerId = "uiux_designer"
            ),
            QuizQuestion(
                id = "q_ux_2",
                question = "What is the minimum recommended touch target size according to Android Material Design 3 and WCAG guidelines?",
                options = listOf("24 x 24 dp", "32 x 32 dp", "48 x 48 dp", "64 x 64 dp"),
                correctIndex = 2,
                explanation = "Material 3 and accessibility standards require a minimum 48x48 dp touch target boundary to prevent mis-taps on mobile devices.",
                careerId = "uiux_designer"
            ),
            QuizQuestion(
                id = "q_ux_3",
                question = "What is the primary objective of a 'Heuristic Evaluation' conducted by UX professionals?",
                options = listOf("Writing production CSS stylesheets", "Generating random user persona demographics", "Measuring server API response latency", "Evaluating an interface against recognized usability principles (e.g. Nielsen's 10 Heuristics)"),
                correctIndex = 3,
                explanation = "Heuristic evaluation inspects UI flows against established usability heuristics to identify user friction and design flaws systematically.",
                careerId = "uiux_designer"
            ),
            QuizQuestion(
                id = "q_ux_4",
                question = "Which phase in the Double Diamond design framework focuses on brainstorming and prototyping multiple potential solutions?",
                options = listOf("Develop", "Discover", "Define", "Deliver"),
                correctIndex = 0,
                explanation = "In the Double Diamond framework, Develop is the second divergence phase where teams iterate, conceptualize, and prototype creative solutions.",
                careerId = "uiux_designer"
            ),
            QuizQuestion(
                id = "q_ux_5",
                question = "What does WCAG 2.1 AA level require for minimum contrast ratio between normal body text and its background?",
                options = listOf("2.5 : 1", "4.5 : 1", "3.0 : 1", "7.0 : 1"),
                correctIndex = 1,
                explanation = "WCAG 2.1 AA mandates a minimum contrast ratio of 4.5:1 for normal body text and 3:1 for large text (18pt / 14pt bold).",
                careerId = "uiux_designer"
            )
        ),

        // Cybersecurity Analyst Quiz
        "cyber_analyst" to listOf(
            QuizQuestion(
                id = "q_cy_1",
                question = "What is the core defensive measure against SQL Injection vulnerabilities in backend web applications?",
                options = listOf("Client-side JavaScript form validation", "Increasing database server RAM", "Parameterized queries / Prepared Statements with Object-Relational Mappers", "Using Base64 encoding on query strings"),
                correctIndex = 2,
                explanation = "Parameterized queries ensure user input is treated strictly as data literals rather than executable SQL code, preventing SQL injection.",
                careerId = "cyber_analyst"
            ),
            QuizQuestion(
                id = "q_cy_2",
                question = "In the CIA Triad of Information Security, what does the 'I' represent?",
                options = listOf("Identification", "Isolation", "Inspection", "Integrity"),
                correctIndex = 3,
                explanation = "Integrity ensures data is accurate, authentic, and protected against unauthorized tampering, modification, or deletion.",
                careerId = "cyber_analyst"
            ),
            QuizQuestion(
                id = "q_cy_3",
                question = "Which type of cyber attack involves intercepting communications between two parties without their knowledge?",
                options = listOf("Man-in-the-Middle (MitM)", "Cross-Site Scripting (XSS)", "Buffer Overflow", "Denial of Service (DoS)"),
                correctIndex = 0,
                explanation = "A Man-in-the-Middle attack occurs when an attacker covertly intercepts, relays, and possibly alters communication between two nodes.",
                careerId = "cyber_analyst"
            ),
            QuizQuestion(
                id = "q_cy_4",
                question = "What is the primary purpose of a Digital Certificate issued by a Certificate Authority (CA)?",
                options = listOf("To compress network payloads", "To bind a public key to an entity's identity and prove server authenticity", "To block all incoming HTTP traffic", "To store user passwords in plaintext"),
                correctIndex = 1,
                explanation = "A digital certificate cryptographically binds a public key to an organization or domain, enabling trust and encrypted TLS connections.",
                careerId = "cyber_analyst"
            ),
            QuizQuestion(
                id = "q_cy_5",
                question = "What principle states that users and applications should be granted only the minimum permissions necessary to complete their job?",
                options = listOf("Defense in Depth", "Security through Obscurity", "Principle of Least Privilege", "Zero Day Hardening"),
                correctIndex = 2,
                explanation = "The Principle of Least Privilege limits access rights for users, accounts, and processes to only those resources strictly required.",
                careerId = "cyber_analyst"
            )
        ),

        // Cloud & DevOps Engineer Quiz
        "cloud_engineer" to listOf(
            QuizQuestion(
                id = "q_cl_1",
                question = "In Container technology, how does Docker container virtualization differ from traditional Virtual Machines (VMs)?",
                options = listOf("Containers include their own complete guest operating system kernel", "Containers can only run on physical bare-metal servers without hypervisors", "Containers do not support networking or persistent storage", "Containers share the host OS kernel and isolate user spaces, making them lightweight"),
                correctIndex = 3,
                explanation = "Docker containers share the host operating system kernel and isolate processes using cgroups and namespaces, achieving faster boot times and lower overhead than VMs.",
                careerId = "cloud_engineer"
            ),
            QuizQuestion(
                id = "q_cl_2",
                question = "What is the primary benefit of 'Infrastructure as Code' (IaC) tools such as Terraform or AWS CloudFormation?",
                options = listOf("Provides version-controlled, reproducible, and automated infrastructure provisioning", "Automates manual UI mouse clicks", "Replaces relational databases with key-value stores", "Translates Java code directly into binary assembly"),
                correctIndex = 0,
                explanation = "IaC enables declarative definitions of cloud infrastructure that can be versioned, tested, and reliably applied across staging and production.",
                careerId = "cloud_engineer"
            ),
            QuizQuestion(
                id = "q_cl_3",
                question = "Which deployment strategy routes a small percentage (e.g. 5%) of live production traffic to a new release to test stability before full rollout?",
                options = listOf("Big Bang Deployment", "Canary Deployment", "Recreate Deployment", "Cold Standby"),
                correctIndex = 1,
                explanation = "Canary deployments expose a new software version to a small cohort of real users to detect regressions early before widespread deployment.",
                careerId = "cloud_engineer"
            ),
            QuizQuestion(
                id = "q_cl_4",
                question = "In Kubernetes, what is the smallest deployable compute unit that encapsulates one or more containers?",
                options = listOf("Node", "Cluster", "Pod", "Service Mesh"),
                correctIndex = 2,
                explanation = "A Pod is the basic execution unit in Kubernetes, encapsulating one or more tightly coupled containers sharing storage and network namespaces.",
                careerId = "cloud_engineer"
            ),
            QuizQuestion(
                id = "q_cl_5",
                question = "What does the 'S' stand for in a CI/CD Pipeline?",
                options = listOf("Continuous System", "Continuous Scheduling", "Centralized Index", "Continuous Integration / Continuous Delivery"),
                correctIndex = 3,
                explanation = "CI/CD stands for Continuous Integration (automated build & test upon commit) and Continuous Delivery/Deployment (automated release).",
                careerId = "cloud_engineer"
            )
        )
    )

    val GLOBAL_INTERVIEW_QUESTIONS: List<InterviewQuestion> = listOf(
        InterviewQuestion(
            id = "iq_hr_01",
            question = "Tell me about yourself and your career focus in engineering.",
            category = "HR",
            answerKey = "Use the Present-Past-Future framework: Current degree/department, key academic achievements & engineering projects, and long-term career ambition in your target domain.",
            tip = "Keep response between 60-90 seconds. Quantify accomplishments where possible.",
            careerId = null
        ),
        InterviewQuestion(
            id = "iq_hr_02",
            question = "Describe a situation where a technical project hit an unexpected bug before a deadline.",
            category = "HR",
            answerKey = "Use the STAR method (Situation, Task, Action, Result). Emphasize systematic root cause analysis, clear mentor communication, prioritization, and lessons learned.",
            tip = "Focus on accountability and constructive problem-solving rather than blaming team members.",
            careerId = null
        ),
        // === SOFTWARE DEVELOPER (software_dev) ===
        InterviewQuestion(
            id = "iq_tech_01",
            question = "Explain Database Normalization (1NF, 2NF, 3NF) and when you would intentionally denormalize.",
            category = "Technical",
            answerKey = "1NF: Atomic column values; 2NF: No partial dependency on composite keys; 3NF: No transitive dependencies. Denormalize in read-heavy analytics/reporting workloads to minimize expensive multi-table JOINs.",
            tip = "Provide a concrete schema example (e.g., Orders and Line Items).",
            careerId = "software_dev"
        ),
        InterviewQuestion(
            id = "iq_sys_01",
            question = "Design a high-throughput URL Shortener service (e.g., bit.ly) - System Architecture.",
            category = "Technical",
            answerKey = "Core components: API Gateway, Base62 token generator with counter service, Redis caching layer, sharded relational/NoSQL datastore, handling HTTP 301 vs 302 redirects.",
            tip = "Clarify read-to-write traffic ratios (typically 100:1 read heavy) before proposing storage sizing.",
            careerId = "software_dev"
        ),
        InterviewQuestion(
            id = "iq_sd_01",
            question = "Describe a challenging bug you encountered in a team codebase and how you resolved it.",
            category = "Behavioral",
            answerKey = "Structure response using STAR: Situation (unreproducible concurrency deadlock in production), Task (isolate root cause without disrupting users), Action (analyzed thread dumps, added reproducible unit test, implemented reentrant lock), Result (resolved latency spike, added regression test).",
            tip = "Highlight systematic troubleshooting and proactive communication with teammates.",
            careerId = "software_dev"
        ),
        InterviewQuestion(
            id = "iq_sd_02",
            question = "What are the core architectural differences and trade-offs between Monolithic and Microservices architectures?",
            category = "Technical",
            answerKey = "Monoliths have single deployment pipelines, shared database schemas, and simple in-memory module calls, but face scaling bottlenecks. Microservices offer autonomous deployments, polyglot persistence, and domain isolation, but introduce distributed latency, network partitions, and data consistency challenges.",
            tip = "Mention Conway's Law and transactional consistency (e.g., Saga pattern) to show real depth.",
            careerId = "software_dev"
        ),
        InterviewQuestion(
            id = "iq_sd_03",
            question = "How do you handle code reviews when a teammate submits code that doesn't follow conventions or has potential security vulnerabilities?",
            category = "Scenario",
            answerKey = "Focus on the code rather than the person. Provide clear explanations citing documentation or architectural decision records (ADRs), offer constructive alternatives, and distinguish between critical security/functional issues vs stylistic preferences.",
            tip = "Emphasize collaboration, empathy, and mentoring rather than gatekeeping.",
            careerId = "software_dev"
        ),

        // === AI / MACHINE LEARNING ENGINEER (aiml_engineer) ===
        InterviewQuestion(
            id = "iq_tech_02",
            question = "How do you mitigate Overfitting in Deep Machine Learning models?",
            category = "Technical",
            answerKey = "Key strategies: L1/L2 weight regularization, Dropout layers, Early Stopping based on validation loss, Data Augmentation, and cross-validation.",
            tip = "Explain the Bias-Variance tradeoff curve on a whiteboard if in person.",
            careerId = "aiml_engineer"
        ),
        InterviewQuestion(
            id = "iq_ai_01",
            question = "Explain the difference between Precision and Recall. In what scenarios would you prioritize one over the other?",
            category = "Technical",
            answerKey = "Precision = TP / (TP + FP); Recall = TP / (TP + FN). Prioritize Recall in critical medical diagnosis or malware detection where missing a true positive is catastrophic. Prioritize Precision in spam filters or video recommendation where false positives degrade user trust.",
            tip = "Mention F-beta scores (e.g. F2 score) when one metric is weighted more heavily.",
            careerId = "aiml_engineer"
        ),
        InterviewQuestion(
            id = "iq_ai_02",
            question = "How does Gradient Descent work, and what advantages do Adam and RMSprop provide over standard SGD?",
            category = "Technical",
            answerKey = "Standard SGD updates weights in the opposite direction of loss gradients with fixed learning rate. Adam (Adaptive Moment Estimation) combines momentum (first moment: moving average of gradients) and adaptive learning rates (second moment: uncentered variance), accelerating convergence and navigating saddle points.",
            tip = "Explain how learning rate schedules and warmup prevent early divergence.",
            careerId = "aiml_engineer"
        ),
        InterviewQuestion(
            id = "iq_ai_03",
            question = "Describe an end-to-end machine learning project you designed and deployed to production.",
            category = "Scenario",
            answerKey = "Present using STAR: Objective/business problem, data collection and cleaning pipeline, feature engineering, model selection and validation benchmark, deployment mechanism (e.g., FastAPI / Docker / ONNX runtime), and post-deployment monitoring for data drift.",
            tip = "Be ready to answer questions about the specific evaluation metrics and production latency constraints.",
            careerId = "aiml_engineer"
        ),
        InterviewQuestion(
            id = "iq_ai_04",
            question = "What is Data Drift vs Concept Drift in production ML models, and how do you detect and remediate it?",
            category = "Technical",
            answerKey = "Data drift refers to changes in the distribution of input features P(X) over time. Concept drift occurs when the mapping between features and target labels P(Y|X) changes. Detect using statistical tests (Kolmogorov-Smirnov, Population Stability Index) and remediate through automated retraining triggers and shadow deployments.",
            tip = "Cite real monitoring tools (e.g. Evidently AI, Great Expectations) for extra credibility.",
            careerId = "aiml_engineer"
        ),

        // === DATA SCIENTIST (data_scientist) ===
        InterviewQuestion(
            id = "iq_ds_01",
            question = "Explain the Bias-Variance tradeoff and how ensemble methods address it.",
            category = "Technical",
            answerKey = "Bias is error from erroneous assumptions (underfitting); Variance is sensitivity to fluctuations in training data (overfitting). Bagging (e.g., Random Forests) trains independent models in parallel to reduce variance without increasing bias. Boosting (e.g., XGBoost, LightGBM) trains sequential models to reduce bias iteratively.",
            tip = "Draw or describe the U-shaped total error curve as a function of model complexity.",
            careerId = "data_scientist"
        ),
        InterviewQuestion(
            id = "iq_ds_02",
            question = "How do you design an A/B test for a major product feature rollout from sample sizing to decision making?",
            category = "Scenario",
            answerKey = "1) Define primary metric (e.g., conversion rate) and guardrail metrics; 2) Determine sample size using baseline conversion, minimum detectable effect (MDE), significance level alpha (0.05), and power beta (0.80); 3) Randomize by user ID; 4) Check for sample ratio mismatch (SRM); 5) Compute p-value and confidence intervals before rolling out.",
            tip = "Emphasize guardrail metrics (e.g. page latency, crash rate) to prove business maturity.",
            careerId = "data_scientist"
        ),
        InterviewQuestion(
            id = "iq_ds_03",
            question = "What steps do you take when your dataset has severe class imbalance (e.g., 99.5% negative)?",
            category = "Technical",
            answerKey = "1) Never use raw accuracy; use PR-AUC, F1-score, and cost-sensitive loss; 2) Apply resampling techniques like SMOTE or stratified subsampling on training data only; 3) Use ensemble models like Balanced Random Forest or focal loss; 4) Calibrate decision thresholds according to business misclassification costs.",
            tip = "Stress that resampling must NEVER be applied to validation or test sets to prevent data leakage.",
            careerId = "data_scientist"
        ),
        InterviewQuestion(
            id = "iq_ds_04",
            question = "Describe a situation where a stakeholder pushed for an analytical conclusion not supported by the data.",
            category = "Behavioral",
            answerKey = "STAR response: Acknowledge business goals, present objective data distributions and confidence intervals clearly with visual charts, walk through alternative hypotheses, and suggest a pilot experiment to test their intuition safely without misrepresenting facts.",
            tip = "Demonstrate integrity, objective communication, and empathy toward business pressures.",
            careerId = "data_scientist"
        ),
        InterviewQuestion(
            id = "iq_ds_05",
            question = "How do you handle multicollinearity among predictor variables in regression analysis?",
            category = "Technical",
            answerKey = "Detect using Variance Inflation Factor (VIF > 5 or 10) and correlation heatmaps. Remediate by dropping redundant features, combining collinear features into a composite index, applying dimensionality reduction (PCA), or using L2 Ridge regularization which stabilizes weight estimates.",
            tip = "Explain how multicollinearity inflates standard errors of coefficients without affecting overall model predictive power.",
            careerId = "data_scientist"
        ),

        // === DATA ANALYST (data_analyst) ===
        InterviewQuestion(
            id = "iq_da_01",
            question = "What are Window Functions in SQL, and how do they differ from GROUP BY?",
            category = "Technical",
            answerKey = "GROUP BY collapses multiple rows into a single aggregated summary row. Window functions (OVER clause) perform calculations across a set of table rows related to the current row while retaining each row's individual identity and detail.",
            tip = "Provide examples like RANK(), DENSE_RANK(), and ROW_NUMBER() and their tie-breaking behaviors.",
            careerId = "data_analyst"
        ),
        InterviewQuestion(
            id = "iq_da_02",
            question = "How do you identify whether a metric change is caused by seasonal trends or a genuine performance drop?",
            category = "Scenario",
            answerKey = "Decompose the time series into trend, seasonal, and residual components. Compare metrics Year-over-Year (YoY) rather than Month-over-Month (MoM) to remove annual seasonality, inspect holiday calendars, and compare performance against a stable unexposed control cohort.",
            tip = "Mention moving averages or STL time-series decomposition.",
            careerId = "data_analyst"
        ),
        InterviewQuestion(
            id = "iq_da_03",
            question = "Walk me through how you would build an executive dashboard for key business performance metrics.",
            category = "Scenario",
            answerKey = "1) Interview key stakeholders to define 3-5 core North Star KPIs; 2) Establish a single source of truth data pipeline; 3) Structure the visual layout hierarchically (summary KPI cards at top, trend charts in middle, drill-down tables at bottom); 4) Include clear filters (date, region, product) and automated data refresh.",
            tip = "Focus on actionable insights: every chart should help the executive make a specific operational decision.",
            careerId = "data_analyst"
        ),
        InterviewQuestion(
            id = "iq_da_04",
            question = "Tell me about a time you found an error or discrepancy in a critical business report.",
            category = "Behavioral",
            answerKey = "STAR response: Identified discrepancy during routine data validation, notified report owners immediately with scope of impact, traced root cause to upstream ETL schema change, implemented automated anomaly checks, and reissued updated numbers with transparent audit notes.",
            tip = "Highlight attention to detail and accountability.",
            careerId = "data_analyst"
        ),
        InterviewQuestion(
            id = "iq_da_05",
            question = "Explain the difference between Correlation and Causation with a practical business example.",
            category = "Technical",
            answerKey = "Correlation means two variables change together; Causation means change in one directly causes change in the other. Example: Marketing email clicks may correlate with high customer purchases simply because loyal customers open more emails (omitted variable: customer brand loyalty), not necessarily because the email caused the purchase. Proving causation requires controlled A/B testing.",
            tip = "Mention confounding variables and Simpson's Paradox.",
            careerId = "data_analyst"
        ),

        // === CLOUD & DEVOPS ENGINEER (cloud_engineer) ===
        InterviewQuestion(
            id = "iq_cl_01",
            question = "Explain the difference between Horizontal and Vertical scaling and when to choose each.",
            category = "Technical",
            answerKey = "Vertical scaling (scaling up) increases CPU/RAM of an existing instance; simple to implement but has hard physical limits and single-point-of-failure risk. Horizontal scaling (scaling out) adds more instance nodes behind a load balancer; provides elasticity, fault tolerance, and near-limitless scale, but requires stateless application architecture.",
            tip = "Mention session state offloading to Redis/Memcached to enable horizontal scaling.",
            careerId = "cloud_engineer"
        ),
        InterviewQuestion(
            id = "iq_cl_02",
            question = "How do Kubernetes Services route traffic to Pods, and what is the difference between ClusterIP, NodePort, and LoadBalancer?",
            category = "Technical",
            answerKey = "Kubernetes uses label selectors to dynamically map Service endpoints to Pod IPs. ClusterIP exposes the Service on an internal cluster-only virtual IP. NodePort exposes the Service on each Node's static port. LoadBalancer provisions an external cloud provider load balancer (e.g. AWS ALB, GCP Load Balancer) that routes to the NodePort/ClusterIP.",
            tip = "Mention kube-proxy and iptables/IPVS rules that perform the actual packet translation.",
            careerId = "cloud_engineer"
        ),
        InterviewQuestion(
            id = "iq_cl_03",
            question = "Describe how you would design a secure, zero-downtime CI/CD deployment pipeline.",
            category = "Scenario",
            answerKey = "1) Source trigger with branch protections and secret scanning; 2) Automated unit/lint tests in isolated runner; 3) Multi-stage Docker build with vulnerability scanning (Trivy/Snyk); 4) Push to private registry; 5) Blue-Green or Canary rollout to Kubernetes; 6) Automated smoke tests and health checks; 7) Automatic rollback on elevated error rates.",
            tip = "Emphasize security gates (static analysis, dependency checks) integrated directly into the pipeline.",
            careerId = "cloud_engineer"
        ),
        InterviewQuestion(
            id = "iq_cl_04",
            question = "Tell me about a production outage or critical infrastructure failure you responded to.",
            category = "Behavioral",
            answerKey = "STAR response: High latency alert triggered by monitoring (Prometheus/PagerDuty), joined incident bridge, inspected Grafana dashboards and Kubernetes pod restarts, identified connection pool exhaustion, scaled replicas and adjusted DB pool size, communicated incident status, and conducted blameless post-mortem.",
            tip = "Demonstrate calm under pressure and blameless post-mortem culture.",
            careerId = "cloud_engineer"
        ),
        InterviewQuestion(
            id = "iq_cl_05",
            question = "What is Infrastructure as Code (IaC) and how do you manage drift in production environments?",
            category = "Technical",
            answerKey = "IaC defines infrastructure declaratively in version-controlled configuration files (Terraform, CloudFormation). Drift occurs when out-of-band manual changes occur in the cloud console. Manage drift through continuous reconciliation pipelines (terraform plan in cron/CI), enforcing read-only production console access, and automated alerting on unmanaged resources.",
            tip = "Mention state locking with remote backends (e.g. S3 + DynamoDB) to prevent concurrent writes.",
            careerId = "cloud_engineer"
        ),

        // === CYBERSECURITY ANALYST (cyber_analyst) ===
        InterviewQuestion(
            id = "iq_cy_01",
            question = "Explain the CIA Triad and give an example of a security control for each pillar.",
            category = "Technical",
            answerKey = "Confidentiality: Data accessible only to authorized entities (Control: AES-256 encryption, access control lists). Integrity: Data is accurate and unaltered (Control: Cryptographic hashing like SHA-256, digital signatures). Availability: Systems and data accessible when needed (Control: Redundant server clusters, DDoS mitigation, regular backups).",
            tip = "Mention that security engineering often involves balancing these three pillars against usability.",
            careerId = "cyber_analyst"
        ),
        InterviewQuestion(
            id = "iq_cy_02",
            question = "What steps would you take immediately upon detecting suspicious lateral movement and privilege escalation in network logs?",
            category = "Scenario",
            answerKey = "1) Isolate affected endpoints from network (disable NIC / segment VLAN) to prevent spread; 2) Preserve volatile memory and logs for forensic chain of custody; 3) Revoke compromised credentials and terminate active sessions; 4) Check SIEM for indicator of compromise (IoC) spread; 5) Escalate to Incident Response lead according to playbook.",
            tip = "Never immediately reboot or power off the machine, as that destroys volatile RAM forensic evidence.",
            careerId = "cyber_analyst"
        ),
        InterviewQuestion(
            id = "iq_cy_03",
            question = "What is the difference between an Intrusion Detection System (IDS) and an Intrusion Prevention System (IPS)?",
            category = "Technical",
            answerKey = "An IDS is passive: it monitors traffic copies via span/tap ports and alerts administrators to suspicious patterns without modifying packets. An IPS is inline: traffic actively flows through it, enabling it to actively block, drop malicious packets, or reset TCP connections in real time.",
            tip = "Explain the risk of false positives in inline IPS causing business service disruption.",
            careerId = "cyber_analyst"
        ),
        InterviewQuestion(
            id = "iq_cy_04",
            question = "What is Phishing, and how do you implement a defense-in-depth strategy against sophisticated spear-phishing?",
            category = "Technical",
            answerKey = "Phishing uses deceptive social engineering to steal credentials or deliver malware. Defense-in-depth layers: 1) Technical: SPF, DKIM, DMARC email authentication, automated attachment sandboxing; 2) Identity: FIDO2/WebAuthn hardware MFA (phishing-resistant); 3) Human: Continuous interactive phishing simulations and security awareness training; 4) Endpoint: EDR monitoring.",
            tip = "Stress that SMS and OTP MFA can be intercepted by reverse proxies, so hardware keys are the gold standard.",
            careerId = "cyber_analyst"
        ),
        InterviewQuestion(
            id = "iq_cy_05",
            question = "Describe how a Security Information and Event Management (SIEM) tool works and how you reduce alert fatigue.",
            category = "Scenario",
            answerKey = "SIEM aggregates logs from firewalls, servers, endpoints, and identity providers, normalizes them, and runs correlation rules to flag multi-vector attacks. To reduce alert fatigue: tune threshold baselines, filter known false positives, group related alerts into incidents, implement SOAR playbooks for tier-1 triage, and prioritize alerts by asset criticality.",
            tip = "Focus on signal-to-noise ratio and quantifiable triage efficiency.",
            careerId = "cyber_analyst"
        ),

        // === UI/UX DESIGNER (uiux_designer) ===
        InterviewQuestion(
            id = "iq_ux_01",
            question = "What is the difference between Qualitative and Quantitative UX research, and how do you combine them?",
            category = "Technical",
            answerKey = "Quantitative research answers 'what' and 'how many' through numerical data (analytics, completion rates, A/B test results). Qualitative research answers 'why' and 'how' through direct observation (interviews, usability testing, think-aloud protocols). Combine them using mixed methods: use quantitative metrics to identify drop-off funnels, and qualitative testing to uncover user reasons.",
            tip = "Use the term 'triangulation' to describe validating qualitative observations with quantitative metrics.",
            careerId = "uiux_designer"
        ),
        InterviewQuestion(
            id = "iq_ux_02",
            question = "How do you design for Accessibility (a11y) to comply with WCAG standards in mobile applications?",
            category = "Technical",
            answerKey = "1) Maintain minimum color contrast ratio (4.5:1 normal text, 3:1 large text); 2) Ensure minimum touch target size (48x48 dp in Android M3); 3) Provide meaningful contentDescription for screen readers (TalkBack); 4) Support dynamic text scaling without layout truncation; 5) Do not convey information using color alone (pair with icons/text).",
            tip = "Mention testing with real screen readers (TalkBack/VoiceOver) rather than relying solely on automated linters.",
            careerId = "uiux_designer"
        ),
        InterviewQuestion(
            id = "iq_ux_03",
            question = "Walk me through how you conduct a Card Sorting study to establish Information Architecture.",
            category = "Scenario",
            answerKey = "1) Identify representative content items on index cards; 2) In Open Card Sorting, participants organize cards into groups and name the categories (informs taxonomy creation); 3) In Closed Card Sorting, participants sort cards into predefined categories (validates existing structure); 4) Analyze similarity matrix and dendrograms to build navigation menus.",
            tip = "Follow up card sorting with tree testing to evaluate navigation effectiveness before designing high-fidelity screens.",
            careerId = "uiux_designer"
        ),
        InterviewQuestion(
            id = "iq_ux_04",
            question = "Tell me about a time when user feedback contradicted your initial design hypothesis.",
            category = "Behavioral",
            answerKey = "STAR response: Designed an innovative swipe-based gesture interaction that felt modern during internal reviews, but usability testing revealed users struggled with discoverability and preferred standard bottom navigation tabs. Welcomed the data, iterated to familiar mental models, and observed task completion improve by 40%.",
            tip = "Demonstrate user-centric humility: 'You are not the user'.",
            careerId = "uiux_designer"
        ),
        InterviewQuestion(
            id = "iq_ux_05",
            question = "How do you create and maintain a Scalable Design System with engineering teams?",
            category = "Technical",
            answerKey = "1) Establish design tokens (colors, typography, spacing, elevations) as the atomic foundation; 2) Build reusable component variants (buttons, inputs, cards) with states (default, hover, focus, disabled); 3) Align naming conventions 1:1 between Figma styles and Compose/CSS tokens; 4) Maintain shared documentation and governance versioning for additions.",
            tip = "Mention how design tokens bridge the communication gap between designers and developers.",
            careerId = "uiux_designer"
        )
    )

    val DEFAULT_PROJECTS: List<ProjectItem> = listOf(
        ProjectItem(
            id = "proj1",
            userId = "usr_student_demo",
            title = "AI Career Guidance & Roadmap Platform",
            description = "End-to-end intelligent student career guidance app with multi-dimensional skill evaluation and milestone tracking.",
            techStack = listOf("Kotlin", "Jetpack Compose", "Clean Architecture", "Supabase"),
            githubUrl = "https://github.com/student/cga-guidance",
            liveDemoUrl = "https://cga.demo.app",
            stars = 38
        ),
        ProjectItem(
            id = "proj2",
            userId = "usr_student_demo",
            title = "Campus Placement Analytics Engine",
            description = "Real-time analytics dashboard evaluating student mock coding test performance and recruiting company requirements.",
            techStack = listOf("Python", "FastAPI", "PostgreSQL", "Pandas"),
            githubUrl = "https://github.com/student/placement-analytics",
            liveDemoUrl = "https://placement-demo.org",
            stars = 24
        ),
        ProjectItem(
            id = "proj3",
            userId = "usr_student_demo",
            title = "Intelligent ATS Resume Parser",
            description = "Extracts entity keywords and calculates compatibility scores between job descriptions and student resumes.",
            techStack = listOf("Python", "SpaCy", "Regex", "Streamlit"),
            githubUrl = "https://github.com/student/ats-parser",
            liveDemoUrl = null,
            stars = 19
        )
    )

    /**
     * Resolves practice problems for a given career.
     * Always includes general DSA, aptitude, and technical questions alongside career-specific items.
     */
    fun getPracticeProblems(careerId: String? = null): List<PracticeProblem> {
        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
        if (canonicalCareer == null) {
            return GLOBAL_PRACTICE_PROBLEMS
        }
        return GLOBAL_PRACTICE_PROBLEMS.filter { problem ->
            problem.careerId == null || problem.careerId == canonicalCareer
        }
    }

    /**
     * Finds a single practice problem by its ID, title, or semantic keywords.
     */
    fun getProblemById(id: String): PracticeProblem? {
        val query = id.trim()
        if (query.isBlank()) return null

        // 1. Direct ID match
        when (query.lowercase()) {
            "p1" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "dsa_01" }
            "p2" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "dsa_02" }
            "p3" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "dsa_03" }
            "p4" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "dsa_04" }
            "p5" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "dsa_05" }
            "p6" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_01" }
            "p7" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_02" }
            "p8" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_03" }
            "p9" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_04" }
            "p10" -> return GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_05" }
        }
        GLOBAL_PRACTICE_PROBLEMS.find { it.id == query }?.let { return it }
        GLOBAL_PRACTICE_PROBLEMS.find { it.id.equals(query, ignoreCase = true) }?.let { return it }

        // 2. Direct title match
        GLOBAL_PRACTICE_PROBLEMS.find { it.title.equals(query, ignoreCase = true) }?.let { return it }

        // 3. Keyword / semantic title matching across categories
        val lower = query.lowercase()
        // Coding & ML
        if (lower.contains("vector dot product") || lower.contains("dot product") || lower.contains("cosine similarity")) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_01" }?.let { return it }
        }
        if (lower.contains("softmax") || lower.contains("cross-entropy") || lower.contains("cross entropy")) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_02" }?.let { return it }
        }
        if (lower.contains("k-means") || lower.contains("kmeans") || lower.contains("clustering convergence")) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_03" }?.let { return it }
        }
        if (lower.contains("convolution") || lower.contains("weight sizing") || (lower.contains("feature map") && lower.contains("dimension"))) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_04" }?.let { return it }
        }
        if (lower.contains("precision-recall") || lower.contains("threshold optimization")) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "aiml_05" }?.let { return it }
        }
        // Technical
        if (lower.contains("b-tree") || lower.contains("b+ tree") || lower.contains("b tree") || (lower.contains("index") && lower.contains("hash"))) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "tech_01" }?.let { return it }
        }
        if (lower.contains("grpc") || (lower.contains("rest") && (lower.contains("protobuf") || lower.contains("protocol") || lower.contains("rpc")))) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "tech_02" }?.let { return it }
        }
        if (lower.contains("mutex") || lower.contains("semaphore") || lower.contains("synchronization")) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "tech_03" }?.let { return it }
        }
        // Aptitude
        if (lower.contains("alternate work") || (lower.contains("time") && lower.contains("work"))) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_01" }?.let { return it }
        }
        if (lower.contains("same suit") || (lower.contains("probability") && lower.contains("card"))) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_02" }?.let { return it }
        }
        if (lower.contains("train") && (lower.contains("crossing") || lower.contains("opposite"))) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_03" }?.let { return it }
        }
        if (lower.contains("discount") || lower.contains("profit and loss") || lower.contains("marked price")) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_04" }?.let { return it }
        }
        if (lower.contains("father and son") || lower.contains("problems on ages") || lower.contains("ratio of the present ages")) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_05" }?.let { return it }
        }
        if (lower.contains("pipes and cisterns") || lower.contains("inflow pipe") || lower.contains("reservoir")) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_06" }?.let { return it }
        }
        if (lower.contains("mixture dilution") || (lower.contains("ratio") && lower.contains("mixture"))) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_07" }?.let { return it }
        }
        if (lower.contains("simple interest") || lower.contains("multiplication of principal")) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_08" }?.let { return it }
        }
        if (lower.contains("batting average") || (lower.contains("average") && lower.contains("batsman"))) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_09" }?.let { return it }
        }
        if (lower.contains("price increase") || (lower.contains("consumption") && lower.contains("petrol"))) {
            GLOBAL_PRACTICE_PROBLEMS.find { it.id == "apt_10" }?.let { return it }
        }

        // 4. Substring matching
        return GLOBAL_PRACTICE_PROBLEMS.find {
            val t = it.title.lowercase()
            t.contains(lower) || lower.contains(t)
        }
    }

    /**
     * Resolves an ID or alias to its authoritative canonical catalog ID.
     */
    fun toCanonicalProblemId(id: String): String {
        return getProblemById(id)?.id ?: id
    }

    /**
     * Resolves a problem by combining title, description, and topic fields.
     */
    fun getProblemByQuery(title: String?, description: String?, topic: String?): PracticeProblem? {
        val combined = listOfNotNull(title, description, topic).joinToString(" ").trim()
        if (combined.isBlank()) return null
        return getProblemById(combined)
    }

    /**
     * Resolves speed quiz questions for a given career.
     * Returns career-specific questions prioritized + general placement questions as supplementary without duplicates.
     * If the career-specific bank is not found, falls back safely to general placement questions.
     */
    fun getQuizQuestions(careerId: String? = null, topic: String? = null): List<QuizQuestion> {
        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
        val careerQuestions = if (canonicalCareer != null) QUIZ_QUESTIONS_MAP[canonicalCareer] ?: emptyList() else emptyList()
        val generalQuestions = QUIZ_QUESTIONS_MAP["general"] ?: emptyList()

        if (canonicalCareer != null && careerQuestions.isNotEmpty()) {
            val combined = (careerQuestions + generalQuestions).distinctBy { it.id.ifBlank { it.question } }
            return combined.take(5)
        }
        return generalQuestions.take(5)
    }

    /**
     * Resolves interview questions for a given career.
     */
    fun getInterviewQuestions(careerId: String? = null): List<InterviewQuestion> {
        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
        if (canonicalCareer == null) {
            return GLOBAL_INTERVIEW_QUESTIONS
        }
        return GLOBAL_INTERVIEW_QUESTIONS.filter { item ->
            item.careerId == null || item.careerId == canonicalCareer
        }
    }
}
