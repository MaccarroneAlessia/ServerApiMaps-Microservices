# Subnet Group per RDS (richiede subnet in almeno 2 AZ)
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
  instance_class       = "db.t3.micro" # Idoneo per il Free Tier AWS
  db_name              = "maps_project_db"
  username             = "ale"
  
  # Usa il valore in chiaro per terraform, ma non verrà stampato nei log grazie a variable "sensitive"
  password             = var.db_password 
  
  parameter_group_name = "default.mysql8.0"
  skip_final_snapshot  = true # Importante per ambienti di test per distruggerlo facilmente (evita errori su terraform destroy)
  publicly_accessible  = false # Best practice di sicurezza: non raggiungibile da internet

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.rds.name
}
