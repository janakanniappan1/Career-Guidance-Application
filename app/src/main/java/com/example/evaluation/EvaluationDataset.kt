package com.example.evaluation

import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.MaterialSourceType
import com.example.domain.model.PdfContentUnit

/**
 * Version-controlled, synthetic evaluation dataset for reproducible benchmarking (Step 10).
 * Contains 30 synthetic questions across PDF textbooks, PPTX lecture slides, and Video transcripts:
 * - 10 Answerable single-material questions
 * - 10 Cross-material answerable questions
 * - 10 Unsupported / out-of-scope / insufficient-evidence questions
 */
object EvaluationDataset {

    // --- SYNTHETIC LEARNING MATERIALS ---

    val pdfMaterial = LearningMaterial(
        id = "synth_mat_pdf",
        name = "Computer Systems & Architecture",
        sourceFileName = "Systems_Architecture.pdf",
        type = LearningMaterialType.TEXTBOOK_PDF,
        sourceType = MaterialSourceType.PDF,
        contentUnits = listOf(
            PdfContentUnit(
                id = "synth_pdf_c1",
                materialId = "synth_mat_pdf",
                documentName = "Systems_Architecture.pdf",
                pageNumber = 12,
                text = "Cache coherence is maintained in multiprocessor architectures using protocols like MESI (Modified, Exclusive, Shared, Invalid) to ensure consistency across L1 and L2 caches.",
                sourceType = MaterialSourceType.PDF
            ),
            PdfContentUnit(
                id = "synth_pdf_c2",
                materialId = "synth_mat_pdf",
                documentName = "Systems_Architecture.pdf",
                pageNumber = 24,
                text = "Virtual memory uses translation lookaside buffers (TLB) to cache page table entries and accelerate virtual-to-physical address translation.",
                sourceType = MaterialSourceType.PDF
            ),
            PdfContentUnit(
                id = "synth_pdf_c3",
                materialId = "synth_mat_pdf",
                documentName = "Systems_Architecture.pdf",
                pageNumber = 38,
                text = "Pipelining hazards include structural hazards from hardware resource conflicts, data hazards from instruction dependencies, and control hazards from branching.",
                sourceType = MaterialSourceType.PDF
            ),
            PdfContentUnit(
                id = "synth_pdf_c4",
                materialId = "synth_mat_pdf",
                documentName = "Systems_Architecture.pdf",
                pageNumber = 55,
                text = "B-Tree database storage indexing guarantees O(log n) search, insert, and delete operations by maintaining balanced multi-way tree nodes on disk.",
                sourceType = MaterialSourceType.PDF
            )
        )
    )

    val pptxMaterial = LearningMaterial(
        id = "synth_mat_pptx",
        name = "Operating Systems Principles",
        sourceFileName = "Operating_Systems.pptx",
        type = LearningMaterialType.SLIDES,
        sourceType = MaterialSourceType.PPTX,
        contentUnits = listOf(
            PdfContentUnit(
                id = "synth_pptx_c1",
                materialId = "synth_mat_pptx",
                documentName = "Operating_Systems.pptx",
                pageNumber = 5,
                slideTitle = "Deadlock Characterization",
                text = "Deadlock requires four simultaneous Coffman conditions: mutual exclusion, hold and wait, no preemption, and circular wait.",
                sourceType = MaterialSourceType.PPTX
            ),
            PdfContentUnit(
                id = "synth_pptx_c2",
                materialId = "synth_mat_pptx",
                documentName = "Operating_Systems.pptx",
                pageNumber = 11,
                slideTitle = "Concurrency & Synchronization Primitives",
                text = "Mutex locks and binary semaphores prevent race conditions by guaranteeing mutual exclusion across concurrent thread execution.",
                sourceType = MaterialSourceType.PPTX
            ),
            PdfContentUnit(
                id = "synth_pptx_c3",
                materialId = "synth_mat_pptx",
                documentName = "Operating_Systems.pptx",
                pageNumber = 19,
                slideTitle = "CPU Scheduling Algorithms",
                text = "Round-robin scheduling allocates fixed time quantum slices to processes in the ready queue, preventing process starvation.",
                sourceType = MaterialSourceType.PPTX
            ),
            PdfContentUnit(
                id = "synth_pptx_c4",
                materialId = "synth_mat_pptx",
                documentName = "Operating_Systems.pptx",
                pageNumber = 27,
                slideTitle = "Paging and Memory Management",
                text = "Page faults occur when a requested memory page is not present in physical RAM, triggering disk paging fetch.",
                sourceType = MaterialSourceType.PPTX
            )
        )
    )

    val videoMaterial = LearningMaterial(
        id = "synth_mat_vid",
        name = "Distributed Systems Lecture",
        sourceFileName = "Distributed_Systems_Lecture.mp4",
        type = LearningMaterialType.LECTURE_VIDEO,
        sourceType = MaterialSourceType.VIDEO,
        contentUnits = listOf(
            PdfContentUnit(
                id = "synth_vid_c1",
                materialId = "synth_mat_vid",
                documentName = "Distributed_Systems_Lecture.mp4",
                pageNumber = 1,
                text = "Raft consensus decomposes state machine replication into leader election, log replication, and safety invariants.",
                sourceType = MaterialSourceType.VIDEO,
                startTimeMs = 120000L, // 02:00
                endTimeMs = 150000L    // 02:30
            ),
            PdfContentUnit(
                id = "synth_vid_c2",
                materialId = "synth_mat_vid",
                documentName = "Distributed_Systems_Lecture.mp4",
                pageNumber = 2,
                text = "The CAP theorem states that a distributed data store can simultaneously provide at most two out of Consistency, Availability, and Partition tolerance.",
                sourceType = MaterialSourceType.VIDEO,
                startTimeMs = 240000L, // 04:00
                endTimeMs = 270000L    // 04:30
            ),
            PdfContentUnit(
                id = "synth_vid_c3",
                materialId = "synth_mat_vid",
                documentName = "Distributed_Systems_Lecture.mp4",
                pageNumber = 3,
                text = "Two-Phase Commit (2PC) guarantees atomic transactions across distributed database nodes through prepare and commit phases.",
                sourceType = MaterialSourceType.VIDEO,
                startTimeMs = 360000L, // 06:00
                endTimeMs = 405000L    // 06:45
            ),
            PdfContentUnit(
                id = "synth_vid_c4",
                materialId = "synth_mat_vid",
                documentName = "Distributed_Systems_Lecture.mp4",
                pageNumber = 4,
                text = "Vector clocks provide partial ordering of events in distributed asynchronous systems without relying on synchronized physical clocks.",
                sourceType = MaterialSourceType.VIDEO,
                startTimeMs = 480000L, // 08:00
                endTimeMs = 525000L    // 08:45
            )
        )
    )

    val allMaterials = listOf(pdfMaterial, pptxMaterial, videoMaterial)

    // --- 30 SYNTHETIC TEST CASES ---

    val testCases = listOf(
        // === GROUP 1: 10 ANSWERABLE SINGLE-SOURCE QUESTIONS ===
        EvaluationTestCase(
            id = "Q_ANS_01",
            query = "cache coherence MESI protocol",
            expectedAnswerKeywords = listOf("MESI", "Modified", "Exclusive", "Shared", "Invalid", "cache"),
            expectedSourceChunkIds = listOf("synth_mat_pdf_p12_c0"),
            expectedSourceLocations = listOf("Systems_Architecture.pdf • Page 12"),
            relevantTopic = "Memory & Architecture",
            isAnswerable = true
        ),
        EvaluationTestCase(
            id = "Q_ANS_02",
            query = "virtual memory TLB address translation",
            expectedAnswerKeywords = listOf("TLB", "translation lookaside buffer", "page table", "virtual"),
            expectedSourceChunkIds = listOf("synth_mat_pdf_p24_c0"),
            expectedSourceLocations = listOf("Systems_Architecture.pdf • Page 24"),
            relevantTopic = "Memory & Architecture",
            isAnswerable = true
        ),
        EvaluationTestCase(
            id = "Q_ANS_03",
            query = "pipelining hazards data control structural",
            expectedAnswerKeywords = listOf("structural hazards", "data hazards", "control hazards"),
            expectedSourceChunkIds = listOf("synth_mat_pdf_p38_c0"),
            expectedSourceLocations = listOf("Systems_Architecture.pdf • Page 38"),
            relevantTopic = "Processor Architecture",
            isAnswerable = true
        ),
        EvaluationTestCase(
            id = "Q_ANS_04",
            query = "B-Tree indexing balanced tree disk",
            expectedAnswerKeywords = listOf("B-Tree", "O(log n)", "balanced", "indexing"),
            expectedSourceChunkIds = listOf("synth_mat_pdf_p55_c0"),
            expectedSourceLocations = listOf("Systems_Architecture.pdf • Page 55"),
            relevantTopic = "Database Indexing",
            isAnswerable = true
        ),
        EvaluationTestCase(
            id = "Q_ANS_05",
            query = "deadlock Coffman conditions circular wait",
            expectedAnswerKeywords = listOf("mutual exclusion", "hold and wait", "no preemption", "circular wait"),
            expectedSourceChunkIds = listOf("synth_mat_pptx_p5_c0"),
            expectedSourceLocations = listOf("Operating_Systems.pptx • Slide 5"),
            relevantTopic = "Concurrency & Deadlocks",
            isAnswerable = true
        ),
        EvaluationTestCase(
            id = "Q_ANS_06",
            query = "mutex locks binary semaphores race conditions",
            expectedAnswerKeywords = listOf("mutex", "semaphores", "mutual exclusion", "race conditions"),
            expectedSourceChunkIds = listOf("synth_mat_pptx_p11_c0"),
            expectedSourceLocations = listOf("Operating_Systems.pptx • Slide 11"),
            relevantTopic = "Synchronization",
            isAnswerable = true
        ),
        EvaluationTestCase(
            id = "Q_ANS_07",
            query = "round-robin scheduling time quantum ready queue",
            expectedAnswerKeywords = listOf("Round-robin", "time quantum", "starvation"),
            expectedSourceChunkIds = listOf("synth_mat_pptx_p19_c0"),
            expectedSourceLocations = listOf("Operating_Systems.pptx • Slide 19"),
            relevantTopic = "CPU Scheduling",
            isAnswerable = true
        ),
        EvaluationTestCase(
            id = "Q_ANS_08",
            query = "Raft consensus leader election log replication",
            expectedAnswerKeywords = listOf("Raft", "leader election", "log replication", "safety"),
            expectedSourceChunkIds = listOf("synth_mat_vid_p1_c0"),
            expectedSourceLocations = listOf("Distributed_Systems_Lecture.mp4 • 02:00–02:30"),
            relevantTopic = "Distributed Consensus",
            isAnswerable = true
        ),
        EvaluationTestCase(
            id = "Q_ANS_09",
            query = "CAP theorem consistency availability partition tolerance",
            expectedAnswerKeywords = listOf("CAP theorem", "Consistency", "Availability", "Partition tolerance"),
            expectedSourceChunkIds = listOf("synth_mat_vid_p2_c0"),
            expectedSourceLocations = listOf("Distributed_Systems_Lecture.mp4 • 04:00–04:30"),
            relevantTopic = "Distributed Systems",
            isAnswerable = true
        ),
        EvaluationTestCase(
            id = "Q_ANS_10",
            query = "Two-Phase Commit 2PC atomic transactions prepare",
            expectedAnswerKeywords = listOf("Two-Phase Commit", "2PC", "prepare", "commit"),
            expectedSourceChunkIds = listOf("synth_mat_vid_p3_c0"),
            expectedSourceLocations = listOf("Distributed_Systems_Lecture.mp4 • 06:00–06:45"),
            relevantTopic = "Distributed Transactions",
            isAnswerable = true
        ),

        // === GROUP 2: 10 CROSS-MATERIAL QUESTIONS ===
        EvaluationTestCase(
            id = "Q_CROSS_01",
            query = "virtual memory page faults paging",
            expectedAnswerKeywords = listOf("virtual memory", "page faults", "RAM", "TLB"),
            expectedSourceChunkIds = listOf("synth_mat_pdf_p24_c0", "synth_mat_pptx_p27_c0"),
            expectedSourceLocations = listOf("Systems_Architecture.pdf • Page 24", "Operating_Systems.pptx • Slide 27"),
            relevantTopic = "Memory Management",
            isAnswerable = true,
            isCrossMaterial = true
        ),
        EvaluationTestCase(
            id = "Q_CROSS_02",
            query = "mutual exclusion locks deadlocks synchronization",
            expectedAnswerKeywords = listOf("mutual exclusion", "mutex", "deadlock"),
            expectedSourceChunkIds = listOf("synth_mat_pptx_p5_c0", "synth_mat_pptx_p11_c0"),
            expectedSourceLocations = listOf("Operating_Systems.pptx • Slide 5", "Operating_Systems.pptx • Slide 11"),
            relevantTopic = "Concurrency",
            isAnswerable = true,
            isCrossMaterial = true
        ),
        EvaluationTestCase(
            id = "Q_CROSS_03",
            query = "database storage indexing transactions two-phase commit",
            expectedAnswerKeywords = listOf("indexing", "transactions", "B-Tree", "Two-Phase Commit"),
            expectedSourceChunkIds = listOf("synth_mat_pdf_p55_c0", "synth_mat_vid_p3_c0"),
            expectedSourceLocations = listOf("Systems_Architecture.pdf • Page 55", "Distributed_Systems_Lecture.mp4 • 06:00–06:45"),
            relevantTopic = "Database & Distributed Systems",
            isAnswerable = true,
            isCrossMaterial = true
        ),
        EvaluationTestCase(
            id = "Q_CROSS_04",
            query = "consistency cache coherence CAP theorem",
            expectedAnswerKeywords = listOf("consistency", "coherence", "CAP"),
            expectedSourceChunkIds = listOf("synth_mat_pdf_p12_c0", "synth_mat_vid_p2_c0"),
            expectedSourceLocations = listOf("Systems_Architecture.pdf • Page 12", "Distributed_Systems_Lecture.mp4 • 04:00–04:30"),
            relevantTopic = "Consistency Models",
            isAnswerable = true,
            isCrossMaterial = true
        ),
        EvaluationTestCase(
            id = "Q_CROSS_05",
            query = "concurrency race conditions synchronization mutex",
            expectedAnswerKeywords = listOf("race conditions", "mutual exclusion", "mutex"),
            expectedSourceChunkIds = listOf("synth_mat_pptx_p11_c0"),
            expectedSourceLocations = listOf("Operating_Systems.pptx • Slide 11"),
            relevantTopic = "Concurrency",
            isAnswerable = true,
            isCrossMaterial = true
        ),
        EvaluationTestCase(
            id = "Q_CROSS_06",
            query = "CPU scheduling processes starvation ready queue",
            expectedAnswerKeywords = listOf("scheduling", "starvation", "ready queue"),
            expectedSourceChunkIds = listOf("synth_mat_pptx_p19_c0"),
            expectedSourceLocations = listOf("Operating_Systems.pptx • Slide 19"),
            relevantTopic = "CPU Scheduling",
            isAnswerable = true,
            isCrossMaterial = true
        ),
        EvaluationTestCase(
            id = "Q_CROSS_07",
            query = "vector clocks ordering events distributed asynchronous",
            expectedAnswerKeywords = listOf("Vector clocks", "partial ordering", "asynchronous"),
            expectedSourceChunkIds = listOf("synth_mat_vid_p4_c0"),
            expectedSourceLocations = listOf("Distributed_Systems_Lecture.mp4 • 08:00–08:45"),
            relevantTopic = "Distributed Systems",
            isAnswerable = true,
            isCrossMaterial = true
        ),
        EvaluationTestCase(
            id = "Q_CROSS_08",
            query = "state machine replication consensus Raft leader",
            expectedAnswerKeywords = listOf("state machine", "Raft", "leader"),
            expectedSourceChunkIds = listOf("synth_mat_vid_p1_c0"),
            expectedSourceLocations = listOf("Distributed_Systems_Lecture.mp4 • 02:00–02:30"),
            relevantTopic = "Distributed Consensus",
            isAnswerable = true,
            isCrossMaterial = true
        ),
        EvaluationTestCase(
            id = "Q_CROSS_09",
            query = "disk paging memory fetch page fault",
            expectedAnswerKeywords = listOf("paging", "page fault", "disk"),
            expectedSourceChunkIds = listOf("synth_mat_pptx_p27_c0"),
            expectedSourceLocations = listOf("Operating_Systems.pptx • Slide 27"),
            relevantTopic = "Memory Management",
            isAnswerable = true,
            isCrossMaterial = true
        ),
        EvaluationTestCase(
            id = "Q_CROSS_10",
            query = "pipelining instruction dependencies data hazards hardware",
            expectedAnswerKeywords = listOf("pipelining", "hazards", "dependencies"),
            expectedSourceChunkIds = listOf("synth_mat_pdf_p38_c0"),
            expectedSourceLocations = listOf("Systems_Architecture.pdf • Page 38"),
            relevantTopic = "Processor Architecture",
            isAnswerable = true,
            isCrossMaterial = true
        ),

        // === GROUP 3: 10 UNSUPPORTED / OUT-OF-SCOPE / INSUFFICIENT-EVIDENCE QUESTIONS ===
        EvaluationTestCase(
            id = "Q_UNSUP_01",
            query = "quantum entanglement Bell inequality qubits",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "Quantum Mechanics",
            isAnswerable = false
        ),
        EvaluationTestCase(
            id = "Q_UNSUP_02",
            query = "photosynthesis Calvin cycle chlorophyll chloroplast",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "Plant Biology",
            isAnswerable = false
        ),
        EvaluationTestCase(
            id = "Q_UNSUP_03",
            query = "French revolution Bastille Louis XVI guillotine",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "European History",
            isAnswerable = false
        ),
        EvaluationTestCase(
            id = "Q_UNSUP_04",
            query = "Black-Scholes option pricing financial derivatives volatility",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "Quantitative Finance",
            isAnswerable = false
        ),
        EvaluationTestCase(
            id = "Q_UNSUP_05",
            query = "CRISPR Cas9 gene editing guide RNA",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "Genetics",
            isAnswerable = false
        ),
        EvaluationTestCase(
            id = "Q_UNSUP_06",
            query = "astrophysics dark energy cosmic expansion Hubble constant",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "Cosmology",
            isAnswerable = false
        ),
        EvaluationTestCase(
            id = "Q_UNSUP_07",
            query = "organic chemistry Diels-Alder cycloaddition dienophile",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "Chemistry",
            isAnswerable = false
        ),
        EvaluationTestCase(
            id = "Q_UNSUP_08",
            query = "microeconomics elasticity consumer surplus indifference curve",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "Economics",
            isAnswerable = false
        ),
        EvaluationTestCase(
            id = "Q_UNSUP_09",
            query = "plate tectonics subduction zone lithosphere mantle convection",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "Geology",
            isAnswerable = false
        ),
        EvaluationTestCase(
            id = "Q_UNSUP_10",
            query = "Shakespearean sonnet iambic pentameter rhyming couplet",
            expectedAnswerKeywords = emptyList(),
            expectedSourceChunkIds = emptyList(),
            expectedSourceLocations = emptyList(),
            relevantTopic = "English Literature",
            isAnswerable = false
        )
    )
}
