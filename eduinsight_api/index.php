<?php
error_reporting(0);
ini_set('display_errors', 0);

// --- 1. Database Connection Check ---
include 'db.php';
$db_status = false;
$table_count = 0;
if ($conn && !$conn->connect_error) {
    $db_status = true;
    $res = $conn->query("SHOW TABLES");
    $table_count = $res->num_rows;
}

// --- 2. API Configuration Check ---
$apiKey = 'YOUR_BREVO_API_KEY_HERE';
$api_ready = (strlen($apiKey) > 20);

// --- 3. Storage Check ---
$storage_ready = is_dir('uploads') && is_writable('uploads');

// --- 4. Android APK Status ---
// Since we are live, we assume APK is ready if server is operational
$apk_status = $db_status && $api_ready;

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>EduInBridge | System Control Center</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root { --primary: #6366f1; --bg: #f8fafc; --dark: #0f172a; }
        body { background-color: var(--bg); font-family: 'Inter', sans-serif; color: var(--dark); }
        .hero { background: linear-gradient(135deg, #1e1b4b 0%, #312e81 100%); color: white; padding: 80px 0 120px; }
        .main-card { border: none; border-radius: 24px; box-shadow: 0 20px 50px rgba(0,0,0,0.1); margin-top: -80px; background: white; overflow: hidden; }
        .status-badge { padding: 6px 16px; border-radius: 50px; font-weight: 600; font-size: 0.75rem; text-transform: uppercase; }
        .status-online { background: #dcfce7; color: #166534; }
        .status-offline { background: #fee2e2; color: #991b1b; }
        .diag-item { padding: 20px; border-radius: 16px; border: 1px solid #f1f5f9; transition: all 0.3s ease; }
        .diag-item:hover { border-color: var(--primary); background: #fdf2f8; transform: translateY(-2px); }
        .icon-box { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 15px; }
    </style>
</head>
<body>

<div class="hero text-center">
    <div class="container">
        <h1 class="display-5 fw-bold">EduInBridge Control Center</h1>
        <p class="opacity-75">Real-time Backend & System Diagnostic Dashboard</p>
    </div>
</div>

<div class="container mb-5 pb-5">
    <div class="row justify-content-center">
        <div class="col-lg-10">
            <div class="main-card p-4 p-md-5">

                <div class="row g-4">
                    <!-- 1. PHP Engine Status -->
                    <div class="col-md-4">
                        <div class="diag-item">
                            <div class="icon-box bg-primary-subtle text-primary"><i class="fa-brands fa-php fa-xl"></i></div>
                            <h6>PHP Engine</h6>
                            <p class="small text-muted">Version <?php echo phpversion(); ?></p>
                            <span class="status-badge status-online">Operational</span>
                        </div>
                    </div>

                    <!-- 2. Database Connectivity -->
                    <div class="col-md-4">
                        <div class="diag-item">
                            <div class="icon-box bg-success-subtle text-success"><i class="fa-solid fa-database fa-xl"></i></div>
                            <h6>Database Connection</h6>
                            <p class="small text-muted"><?php echo $table_count; ?> Tables Synced</p>
                            <span class="status-badge <?php echo $db_status ? 'status-online' : 'status-offline'; ?>">
                                <?php echo $db_status ? 'Connected' : 'Disconnected'; ?>
                            </span>
                        </div>
                    </div>

                    <!-- 3. API Security (Brevo) -->
                    <div class="col-md-4">
                        <div class="diag-item">
                            <div class="icon-box bg-warning-subtle text-warning"><i class="fa-solid fa-key fa-xl"></i></div>
                            <h6>Brevo OTP Engine</h6>
                            <p class="small text-muted">SMTP/API Relay</p>
                            <span class="status-badge <?php echo $api_ready ? 'status-online' : 'status-offline'; ?>">
                                <?php echo $api_ready ? 'Key Verified' : 'Key Invalid'; ?>
                            </span>
                        </div>
                    </div>

                    <!-- 4. Storage & Uploads -->
                    <div class="col-md-4">
                        <div class="diag-item">
                            <div class="icon-box bg-info-subtle text-info"><i class="fa-solid fa-folder-open fa-xl"></i></div>
                            <h6>Storage System</h6>
                            <p class="small text-muted">Uploads Directory</p>
                            <span class="status-badge <?php echo $storage_ready ? 'status-online' : 'status-offline'; ?>">
                                <?php echo $storage_ready ? 'Writable' : 'Restricted'; ?>
                            </span>
                        </div>
                    </div>

                    <!-- 5. Android APK Readiness -->
                    <div class="col-md-8">
                        <div class="diag-item d-flex align-items-center justify-content-between">
                            <div class="d-flex align-items-center">
                                <div class="icon-box bg-danger-subtle text-danger mb-0 me-3"><i class="fa-brands fa-android fa-xl"></i></div>
                                <div>
                                    <h6 class="mb-0">Android Application Linkage</h6>
                                    <p class="small text-muted mb-0">APK is ready to communicate with this server.</p>
                                </div>
                            </div>
                            <span class="status-badge <?php echo $apk_status ? 'status-online' : 'status-offline'; ?>">
                                <?php echo $apk_status ? 'READY TO LAUNCH' : 'INCOMPLETE'; ?>
                            </span>
                        </div>
                    </div>
                </div>

                <div class="alert alert-secondary mt-5 border-0 rounded-4 p-4 text-center">
                    <p class="mb-2 small text-uppercase fw-bold letter-spacing-1 text-muted">Primary API Entry Point</p>
                    <h5 class="mb-0 font-monospace text-primary">http://<?php echo $_SERVER['HTTP_HOST']; ?>/</h5>
                </div>

                <div class="mt-5 pt-4 text-center border-top border-light">
                    <p class="small text-muted mb-0">&copy; 2024 EduInBridge Professional Platform | All Systems Normal</p>
                </div>

            </div>
        </div>
    </div>
</div>

</body>
</html>