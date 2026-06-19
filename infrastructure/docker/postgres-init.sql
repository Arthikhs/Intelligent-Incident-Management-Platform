-- Create all required databases on first startup
CREATE DATABASE iimp_incidents;
CREATE DATABASE iimp_users;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE iimp_incidents TO iimp;
GRANT ALL PRIVILEGES ON DATABASE iimp_users TO iimp;
