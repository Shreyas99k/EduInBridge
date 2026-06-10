<?php
header('Content-Type: application/json');

//1. Database Connection
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    echo json_encode([]); // Return empty array on failure
    exit();
}

// 2. Fetch all users with the role 'institution' including their address
$sql = "SELECT id, name, address FROM users WHERE role = 'institution' ORDER BY name ASC";
$result = $conn->query($sql);

$institutions = [];
if ($result && $result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        // If address is null, provide a fallback
        if (!isset($row['address']) || empty($row['address'])) {
            $row['address'] = "Official Education Node";
        }
        $institutions[] = $row;
    }
}

// 3. Return the list as a JSON array
echo json_encode($institutions);

$conn->close();
?>