<?php
header('Content-Type: application/json');
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

$type = $_GET['type'] ?? '';
$inst_id = $_GET['institution_id'] ?? '';
$dept_id = $_GET['department_id'] ?? '';
$subject = $_GET['subject_name'] ?? '';

// Fetch materials and join with users to get the uploader's name
$sql = "SELECT m.*, u.name as uploader_name 
        FROM materials m 
        LEFT JOIN users u ON m.uploader_id = u.id 
        WHERE m.institution_id = ? AND m.type = ? AND m.department_id = ? AND m.subject_name = ?
        ORDER BY m.upload_date DESC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("isis", $inst_id, $type, $dept_id, $subject);
$stmt->execute();
$result = $stmt->get_result();

$materials = [];
while($row = $result->fetch_assoc()) {
    $materials[] = [
        "file_name" => $row['file_name'],
        "subject_name" => $row['subject_name'],
        "uploader_name" => $row['uploader_name'] ?? 'System',
        "uploaded_at" => $row['upload_date'],
        "file_path" => str_replace("uploads/", "", $row['file_path']) // Strip 'uploads/' if it's already in the path
    ];
}

echo json_encode($materials);
$conn->close();
?>