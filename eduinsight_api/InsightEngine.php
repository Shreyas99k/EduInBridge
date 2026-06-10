<?php
// InsightEngine.php logic
function calculateInsightScore($student_id, $conn) {
    // 1. Fetch current metrics
    $query = "SELECT marks_avg, attendance_avg, behavior_pts FROM students WHERE student_id = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("i", $student_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($row = $result->fetch_assoc()) {
        $marks = $row['marks_avg'];
        $attendance = $row['attendance_avg'];
        $behavior = $row['behavior_pts'];

        // 2. Simple Formula: (Marks + Attendance + Behavior Factor) / Max
        // Adjust this formula to your needs
        $new_score = ($marks * 0.4) + ($attendance * 0.4) + ($behavior * 0.2);
        
        if ($new_score > 100) $new_score = 100;

        // 3. Save the new calculated score back to the student table
        $update = $conn->prepare("UPDATE students SET insight_score = ? WHERE student_id = ?");
        $update->bind_param("di", $new_score, $student_id);
        $update->execute();
    }
}
?>