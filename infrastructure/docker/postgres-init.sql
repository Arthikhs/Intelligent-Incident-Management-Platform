-- Create all required databases on first startup
CREATE DATABASE iimp_incidents;
CREATE DATABASE iimp_users;
CREATE DATABASE iimp_notifications;
CREATE DATABASE iimp_anomaly;
CREATE DATABASE iimp_blast;
CREATE DATABASE iimp_slo;
CREATE DATABASE iimp_postmortem;
CREATE DATABASE iimp_kb;
CREATE DATABASE iimp_copilot;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE iimp_incidents TO iimp;
GRANT ALL PRIVILEGES ON DATABASE iimp_users TO iimp;
GRANT ALL PRIVILEGES ON DATABASE iimp_notifications TO iimp;
GRANT ALL PRIVILEGES ON DATABASE iimp_anomaly TO iimp;
GRANT ALL PRIVILEGES ON DATABASE iimp_blast TO iimp;
GRANT ALL PRIVILEGES ON DATABASE iimp_slo TO iimp;
GRANT ALL PRIVILEGES ON DATABASE iimp_postmortem TO iimp;
GRANT ALL PRIVILEGES ON DATABASE iimp_kb TO iimp;
GRANT ALL PRIVILEGES ON DATABASE iimp_copilot TO iimp;
