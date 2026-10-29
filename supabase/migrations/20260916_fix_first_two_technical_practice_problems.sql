-- ==============================================================================
-- FIX ONLY THE FIRST TWO TECHNICAL PRACTICE QUESTIONS
-- 1. Updates Database Indexing / B-Tree vs Hash Index (tech_01)
-- 2. Updates REST vs gRPC & Protocol Buffers (tech_02)
-- 3. Updates AIML technical questions (aiml_02, aiml_04)
-- Ensures options are stored in the exact correct JSONB array format,
-- correct_index is valid (1), explanation and hint are present,
-- difficulty and xp_reward (+20 / +30 XP) are preserved.
-- ==============================================================================

-- 1. Database Indexing & B-Tree vs Hash Index (tech_01 or matching title)
UPDATE public.practice_problems
SET 
    options = '["A) Hash indexes maintain keys in alphabetical sorted order, making them faster for range queries", "B) B+ Trees keep keys sorted and link leaf nodes sequentially, enabling efficient range traversal in O(log N + K) time", "C) B+ Trees have an O(1) worst-case lookup time for every query type", "D) Hash indexes require fewer disk I/O operations than B+ Trees for range scanning queries"]'::jsonb,
    correct_index = 1,
    explanation = COALESCE(NULLIF(explanation, ''), 'B+ Trees store index entries in sorted order and connect leaf nodes with bidirectional linked pointers. For range queries (BETWEEN, >, <), the B+ Tree performs an O(log N) tree traversal to find the range start and then traverses leaf pages sequentially. Hash indexes use hash functions that scatter values randomly into buckets, only supporting exact equality (=) lookups.'),
    hint = COALESCE(NULLIF(hint, ''), 'B+ Trees maintain keys in sorted order and link leaf pages sequentially, supporting range scans. Hash indexes scatter keys randomly into buckets without ordering.')
WHERE id = 'tech_01' 
   OR title ILIKE '%Database Indexing%' 
   OR title ILIKE '%B-Tree%' 
   OR title ILIKE '%B+ Tree%';

-- 2. REST vs gRPC & Protocol Buffers (tech_02 or matching title)
UPDATE public.practice_problems
SET 
    options = '["A) JSON text parsing is inherently faster and more compact than binary deserialization", "B) gRPC uses compact binary Protocol Buffers and HTTP/2 multiplexing, reducing payload size and connection overhead", "C) REST over HTTP/1.1 provides built-in bidirectional streaming and header compression by default", "D) Protocol Buffers transmit data without any predefined schemas or type contracts"]'::jsonb,
    correct_index = 1,
    explanation = COALESCE(NULLIF(explanation, ''), 'Protocol Buffers serialize structured data into compact binary payloads that are much smaller and drastically faster to serialize/deserialize than text-based JSON. Furthermore, gRPC uses HTTP/2, which provides bidirectional streaming, header compression (HPACK), and multiplexing of concurrent requests over a single TCP connection.'),
    hint = COALESCE(NULLIF(hint, ''), 'Protocol Buffers serialize data into compact binary representations rather than human-readable JSON text, and HTTP/2 supports multiplexing multiple requests over a single TCP connection.')
WHERE id = 'tech_02' 
   OR title ILIKE '%REST vs gRPC%' 
   OR title ILIKE '%Protocol Buffers%'
   OR title ILIKE '%gRPC%';

-- 3. AIML Technical Question 1 (aiml_02)
UPDATE public.practice_problems
SET 
    title = 'Cross-Entropy Loss & Softmax Computation',
    category = 'Technical',
    difficulty = 'Medium',
    description = 'A neural network classifier outputs uncalibrated real-valued logits z = [2.0, 1.0, 0.1] for C = 3 mutually exclusive classes. The model needs to convert these logits into a valid probability distribution p and compute the loss for a ground-truth one-hot label y where class 0 is the true target (y = [1, 0, 0]).

Which combination of activation function and loss function is the industry standard for multi-class classification, and why?

Select the correct answer from the four options below.',
    career_id = 'aiml_engineer',
    topic = 'Deep Learning Fundamentals',
    xp_reward = 20,
    example_input = 'Classification with C classes, unnormalized output logits z = [z_1, ..., z_C]',
    example_output = 'Normalized class probability vector and scalar cross-entropy loss',
    explanation = 'Softmax converts logits into probabilities via p_i = exp(z_i) / sum(exp(z_j)). Categorical Cross-Entropy loss L = -sum(y_i * log(p_i)) penalizes incorrect predictions logarithmically, maximizing the log-likelihood of the ground-truth class. Its gradient (p - y) is simple and numerically stable during backpropagation.',
    constraints = 'Number of classes C >= 2. Probability distribution must sum to 1.0.',
    hint = 'Softmax normalizes exponential logits into probabilities that sum to 1. Categorical Cross-Entropy takes the negative natural logarithm of the predicted probability for the true class.',
    options = '["A) Sigmoid activation on each output node with Mean Squared Error (MSE) loss", "B) Softmax activation to generate a probability distribution with Categorical Cross-Entropy loss", "C) ReLU activation across output nodes with Hinge Loss", "D) Softmax activation with Mean Absolute Error (MAE) loss"]'::jsonb,
    correct_index = 1
WHERE id = 'aiml_02';

-- 4. AIML Technical Question 2 (aiml_04)
UPDATE public.practice_problems
SET 
    title = 'Convolution Output Dimensions & Weight Sizing',
    category = 'Technical',
    difficulty = 'Easy',
    description = 'A 2D convolutional layer receives an input image volume of dimensions 32 x 32 x 3 (Height x Width x Channels). The layer applies 16 filters, each of size 3 x 3, with a stride of S = 1 and zero padding of P = 0.

What are the spatial height, spatial width, and channel depth dimensions (H_out x W_out x C_out) of the resulting output feature map?

Select the correct answer from the four options below.',
    career_id = 'aiml_engineer',
    topic = 'Computer Vision & CNNs',
    xp_reward = 20,
    example_input = 'Input tensor: 32x32x3, Filters: 16 kernels of size 3x3, Stride: 1, Padding: 0',
    example_output = 'Spatial dimensions (Height x Width x Channels) of output feature tensor',
    explanation = 'Applying the formula: Height_out = floor((32 - 3 + 2*0) / 1) + 1 = 29 + 1 = 30. Width_out = floor((32 - 3 + 2*0) / 1) + 1 = 30. The number of filters is 16, so the output depth C_out = 16. The final output tensor shape is 30 x 30 x 16.',
    constraints = 'Standard 2D convolution arithmetic without dilation (Dilation = 1).',
    hint = 'Use the spatial dimension formula: Output = floor((W - K + 2*P) / S) + 1. The output channel depth always equals the number of convolutional filters applied.',
    options = '["A) 32 x 32 x 16", "B) 30 x 30 x 16", "C) 28 x 28 x 3", "D) 16 x 16 x 32"]'::jsonb,
    correct_index = 1
WHERE id = 'aiml_04';
