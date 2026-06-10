<?php
include 'db.php';
header('Content-Type: application/json');

// This query is the 'Magic Link'
// It selects everything from doubts AND the 'name' from users
$sql = "SELECT doubts.*, users.name AS student_real_name 
        FROM doubts 
        JOIN users ON doubts.student_id = users.id 
        WHERE doubts.status = 'pending' 
        ORDER BY doubts.created_at DESC";

$result = $conn->query($sql);
$doubts = array();

if ($result) {
    while ($row = $result->fetch_assoc()) {
        $doubts[] = $row;
    }
    echo json_encode(["status" => "success", "doubts" => $doubts]);
} else {
    echo json_encode(["status" => "error", "message" => $conn->error]);
}
?>