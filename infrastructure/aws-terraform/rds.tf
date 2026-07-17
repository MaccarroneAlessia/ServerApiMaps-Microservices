# Subnet Group for RDS (requires subnets in at least 2 AZs)
resource "aws_db_subnet_group" "rds" {
  name       = "maps-rds-subnet-group"
  subnet_ids = [aws_subnet.private_1.id, aws_subnet.private_2.id]

  tags = {
    Name = "maps-rds-subnet-group"
  }
}

resource "aws_db_instance" "mysql" {
  identifier           = "maps-database"
  allocated_storage    = 20
  storage_type         = "gp2"
  engine               = "mysql"
  engine_version       = "8.0"
  instance_class       = "db.t3.micro" # Eligible for AWS Free Tier
  db_name              = "maps_project_db"
  username             = "ale"
  
  # Uses the plaintext value for terraform, but it won't be printed in logs thanks to the "sensitive" variable
  password             = var.db_password 
  
  parameter_group_name = "default.mysql8.0"
  skip_final_snapshot  = true # Important for test environments to easily destroy it (avoids terraform destroy errors)
  publicly_accessible  = false # Security best practice: not reachable from the internet

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.rds.name
}
