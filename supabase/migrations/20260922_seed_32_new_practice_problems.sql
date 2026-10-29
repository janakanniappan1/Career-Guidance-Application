-- ==============================================================================
-- MIGRATION: SEED 32 NEW CANONICAL PRACTICE PROBLEMS (METADATA ONLY)
-- Ensures canonical problem IDs from DefaultPracticeCatalog.kt exist
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

-- 3. Insert or Update Practice Problems
INSERT INTO public.practice_problems (
    id, title, category, difficulty, description, career_id, topic, xp_reward, acceptance_rate
) VALUES
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
