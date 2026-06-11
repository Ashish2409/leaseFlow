# LeaseFlow Database Design

# Conventions

* UUID Primary Keys
* snake_case Naming
* created_at
* updated_at

---

# tenants

id UUID PK

name VARCHAR(255)

subscription_plan VARCHAR(50)

created_at TIMESTAMP

updated_at TIMESTAMP

---

# users

id UUID PK

tenant_id UUID FK

email VARCHAR(255)

password_hash VARCHAR(255)

enabled BOOLEAN

created_at TIMESTAMP

updated_at TIMESTAMP

---

# roles

id UUID PK

name VARCHAR(100)

---

# user_roles

user_id UUID FK

role_id UUID FK

PRIMARY KEY(user_id, role_id)

---

# properties

id UUID PK

tenant_id UUID FK

name VARCHAR(255)

address_line1 VARCHAR(255)

city VARCHAR(100)

state VARCHAR(100)

zip_code VARCHAR(20)

created_at TIMESTAMP

updated_at TIMESTAMP

---

# units

id UUID PK

property_id UUID FK

unit_number VARCHAR(50)

bedrooms INTEGER

bathrooms INTEGER

monthly_rent DECIMAL(12,2)

status VARCHAR(50)

created_at TIMESTAMP

updated_at TIMESTAMP

---

# prospects

id UUID PK

tenant_id UUID FK

first_name VARCHAR(100)

last_name VARCHAR(100)

email VARCHAR(255)

phone VARCHAR(50)

status VARCHAR(50)

created_at TIMESTAMP

updated_at TIMESTAMP

---

# applications

id UUID PK

tenant_id UUID FK

prospect_id UUID FK

status VARCHAR(50)

submitted_at TIMESTAMP

created_at TIMESTAMP

updated_at TIMESTAMP

---

# residents

id UUID PK

tenant_id UUID FK

first_name VARCHAR(100)

last_name VARCHAR(100)

email VARCHAR(255)

phone VARCHAR(50)

created_at TIMESTAMP

updated_at TIMESTAMP

---

# leases

id UUID PK

tenant_id UUID FK

resident_id UUID FK

unit_id UUID FK

lease_start DATE

lease_end DATE

monthly_rent DECIMAL(12,2)

security_deposit DECIMAL(12,2)

status VARCHAR(50)

created_at TIMESTAMP

updated_at TIMESTAMP

---

# lease_templates

id UUID PK

tenant_id UUID FK

name VARCHAR(255)

version INTEGER

content TEXT

active BOOLEAN

created_at TIMESTAMP

updated_at TIMESTAMP

---

# lease_documents

id UUID PK

lease_id UUID FK

document_type VARCHAR(100)

file_path VARCHAR(500)

version INTEGER

created_at TIMESTAMP

updated_at TIMESTAMP

---

# signature_requests

id UUID PK

lease_document_id UUID FK

status VARCHAR(50)

sent_at TIMESTAMP

completed_at TIMESTAMP

created_at TIMESTAMP

updated_at TIMESTAMP

---

# notifications

id UUID PK

tenant_id UUID FK

recipient_email VARCHAR(255)

subject VARCHAR(255)

status VARCHAR(50)

created_at TIMESTAMP

updated_at TIMESTAMP

---

# audit_logs

id UUID PK

tenant_id UUID FK

entity_type VARCHAR(100)

entity_id UUID

action VARCHAR(100)

performed_by UUID

performed_at TIMESTAMP

details JSONB
