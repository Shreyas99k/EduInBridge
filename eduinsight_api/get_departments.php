<?php
header('Content-Type: application/json');

// 1. Database Connection
// Use the same credentials found in your get_institutions.php
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Connection failed"]);
    exit();
}

// 2. Get Institution ID from the request
$institution_id = isset($_GET['institution_id']) ? $_GET['institution_id'] : '';

if (empty($institution_id)) {
    echo json_encode([]); // Return empty array if no ID provided
    exit();
}

// 3. Fetch departments for this specific institution
// Note: This assumes you have a table named 'departments' with an 'institution_id' column
$stmt = $conn->prepare("SELECT id, name FROM departments WHERE institution_id = ? ORDER BY name ASC");
$stmt->bind_param("s", $institution_id);
$stmt->execute();
$result = $stmt->get_result();

$departments = array();

while($row = $result->fetch_assoc()) {
    $departments[] = array(
        "id" => $row["id"],
        "name" => $row["name"]
    );
}

// 4. Output as JSON array (matches the Android JSONArray parser)
echo json_encode($departments);

$stmt->close();
$conn->close();
?>