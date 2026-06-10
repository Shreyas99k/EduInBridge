<?php
include 'db.php';
header('Content-Type: application/json');

// Get the student ID from the app request
$student_id = $_GET['student_id'] ?? '';

if (!empty($student_id)) {
    // UPDATED: Added 'rating' to the SELECT query
    $sql = "SELECT 
                id, 
                subject, 
                description, 
                solution, 
                teacher_name, 
                status, 
                created_at, 
                sol_image,
                rating 
            FROM doubts 
            WHERE student_id = ? 
            ORDER BY created_at DESC";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $student_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $history = array();
    while ($row = $result->fetch_assoc()) {
        // Clean the data: If a field is NULL in MySQL, send an empty string instead
        $row['solution'] = $row['solution'] ?? "";
        $row['teacher_name'] = $row['teacher_name'] ?? "Mentor";
        $row['status'] = $row['status'] ?? "pending";
        // Ensure rating is sent as an integer (default to 5)
        $row['rating'] = isset($row['rating']) ? (int)$row['rating'] : 5;
        
        $history[] = $row;
    }

    // Success response - keeping your original structure
    echo json_encode([
        "status" => "success", 
        "history" => $history
    ]);

} else {
    echo json_encode([
        "status" => "error", 
        "message" => "No Student ID provided"
    ]);
}
?>