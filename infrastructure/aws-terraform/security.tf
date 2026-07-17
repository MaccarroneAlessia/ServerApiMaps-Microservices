# Security Group per i Nodi K3s (EC2)
resource "aws_security_group" "k3s_nodes" {
  name        = "maps-k3s-sg"
  description = "Security Group per i nodi EC2 K3s"
  vpc_id      = aws_vpc.main.id

  # Accesso SSH per Ansible
  ingress {
    description = "SSH da Internet"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Accesso API Kubernetes (K3s) da Internet per permettere al Worker di unirsi
  ingress {
    description = "K3s API da Internet"
    from_port   = 6443
    to_port     = 6443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Traffico interno al VPC (comunicazione K3s tra master e worker, es. per Flannel e API K8s su 6443)
  ingress {
    description = "Traffico interno al VPC"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }

  # Accesso all'app da NodePort tramite ALB
  ingress {
    description = "Traffico dal ALB verso NodePort 30080"
    from_port   = 30080
    to_port     = 30080
    protocol    = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    description = "Uscita verso Internet libera"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Security Group per l'Application Load Balancer
resource "aws_security_group" "alb" {
  name        = "maps-alb-sg"
  description = "Consenti traffico HTTP in ingresso al ALB"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP da Internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Traffico in uscita dal ALB"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Security Group per RDS MySQL
resource "aws_security_group" "rds" {
  name        = "maps-rds-sg"
  description = "Consenti traffico in ingresso verso MySQL dai nodi K3s"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Traffico da EC2 su porta 3306"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    security_groups = [aws_security_group.k3s_nodes.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
