<?php
header('Content-Type: application/json');

// 1. Database Connection
$conn = new mysqli("localhost", "YOUR_DB_USER", "YOUR_DB_PASSWORD", "YOUR_DB_NAME");

if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Database connection failed"]);
    exit();
}

// 2. Get Parameters
$teacher_name = trim($_GET['teacher_name'] ?? '');
$requested_status = strtolower(trim($_GET['status'] ?? '')); 

if (empty($teacher_name)) {
    echo json_encode(["status" => "error", "message" => "Teacher name required"]);
    exit();
}

try {
    $doubts = [];
    $all_doubts = [];
    $pending_count = 0;
    $solved_count = 0;
    $unread_messages_count = 0;

    // 3. Fetch ALL doubts first
    $sqlAll = "SELECT * FROM doubts WHERE LOWER(TRIM(teacher_name)) = LOWER(TRIM(?)) ORDER BY id DESC";
    $stmt1 = $conn->prepare($sqlAll);
    if (!$stmt1) {
        throw new Exception("SQL Prepare Failed: " . $conn->error);
    }
    $stmt1->bind_param("s", $teacher_name);
    $stmt1->execute();
    $res1 = $stmt1->get_result();
    while ($row = $res1->fetch_assoc()) {
        $all_doubts[] = $row;
        if ((int)($row['has_new_message'] ?? 0) === 1) {
            $unread_messages_count++;
        }
    }
    $stmt1->close();

    // 4. Fetch Teacher Info (By Name or ID if possible)
    $inst_name = "";
    $dept_name = "";
    $qInfo = "SELECT i.name as inst, d.name as dept, u.branch
              FROM users u
              LEFT JOIN users i ON u.institution_id = i.id
              LEFT JOIN departments d ON u.department_id = d.id
              WHERE LOWER(TRIM(u.name)) = LOWER(TRIM(?)) AND u.role = 'teacher' LIMIT 1";
    $stmt3 = $conn->prepare($qInfo);
    if ($stmt3) {
        $stmt3->bind_param("s", $teacher_name);
        $stmt3->execute();
        $info_res = $stmt3->get_result()->fetch_assoc();
        if ($info_res) {
            $inst_name = $info_res['inst'] ?? "";
            $dept_name = $info_res['dept'] ?? $info_res['branch'] ?? "";
        }
        $stmt3->close();
    }

    // 5. Pre-check for mentorship_chats table
    $table_exists = $conn->query("SHOW TABLES LIKE 'mentorship_chats'")->num_rows > 0;
    $col_exists = false;
    if ($table_exists) {
        $col_exists = $conn->query("SHOW COLUMNS FROM mentorship_chats LIKE 'doubt_id'")->num_rows > 0;
    }

    // 6. Process the doubts
    foreach ($all_doubts as $row) {
        $row['id'] = (int)$row['id'];
        $row['rating'] = (int)($row['rating'] ?? 0);
        
        $status = strtolower($row['status'] ?? 'pending');
        
        if ($status == 'solved') {
            $solved_count++;
        } else {
            $pending_count++;
        }

        $shouldAddToList = false;
        if ($requested_status !== '') {
            if ($status === $requested_status) $shouldAddToList = true;
        } else {
            if ($status !== 'solved') $shouldAddToList = true;
        }

        if ($shouldAddToList) {
            $row['teacher_replied_count'] = 0;
            if ($table_exists && $col_exists) {
                $d_id = $row['id'];
                $chat_res = $conn->query("SELECT COUNT(*) as cnt FROM mentorship_chats WHERE doubt_id = $d_id AND sender_role = 'teacher'");
                if ($chat_res) {
                    $chat_data = $chat_res->fetch_assoc();
                    $row['teacher_replied_count'] = (int)$chat_data['cnt'];
                }
            }
            $doubts[] = $row;
        }
    }

    // 7. Average Rating calculation
    $average_rating = 5.0;
    $qRating = "SELECT AVG(rating) as avg_r FROM doubts WHERE LOWER(TRIM(teacher_name)) = LOWER(TRIM(?)) AND status = 'solved' AND rating > 0";
    $stmt2 = $conn->prepare($qRating);
    if ($stmt2) {
        $stmt2->bind_param("s", $teacher_name);
        $stmt2->execute();
        $rating_res = $stmt2->get_result()->fetch_assoc();
        if ($rating_res && $rating_res['avg_r']) {
            $average_rating = round($rating_res['avg_r'], 1);
        }
        $stmt2->close();
    }

    echo json_encode([
        "status" => "success",
        "pending_count" => (int)$pending_count,
        "solved_count" => (int)$solved_count,
        "unread_count" => (int)$unread_messages_count,
        "rating" => (float)$average_rating,
        "institution_name" => $inst_name,
        "department_name" => $dept_name,
        "doubts" => $doubts
    ]);

} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage(), "doubts" => []]);
}

$conn->close();
?>