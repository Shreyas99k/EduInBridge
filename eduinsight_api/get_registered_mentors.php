<?php
header('Content-Type: application/json');
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    die(json_encode([]));
}

$inst_id = $_GET['institution_id'] ?? '';

// JOIN users with departments to get the registered department name
$sql = "SELECT u.id, u.name, u.email, u.mobile, 
               COALESCE(d.name, u.branch, 'Not Provided') as branch 
        FROM users u 
        LEFT JOIN departments d ON u.department_id = d.id 
        WHERE u.institution_id = ? AND u.role = 'teacher' AND u.status = 'active'";

$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $inst_id);
$stmt->execute();
$result = $stmt->get_result();

$mentors = [];
while($row = $result->fetch_assoc()) {
    $mentors[] = $row;
}

echo json_encode($mentors);
$stmt->close();
$conn->close();
?>