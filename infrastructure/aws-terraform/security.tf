# Security Group for K3s Nodes (EC2)
resource "aws_security_group" "k3s_nodes" {
  name        = "maps-k3s-sg"
  description = "Security Group for K3s EC2 nodes"
  vpc_id      = aws_vpc.main.id

  # SSH Access for Ansible
  ingress {
    description = "SSH from Internet"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Kubernetes API (K3s) Access from Internet to allow Worker to join
  ingress {
    description = "K3s API from Internet"
    from_port   = 6443
    to_port     = 6443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Internal VPC traffic (K3s communication between master and worker, e.g. for Flannel and K8s API on 6443)
  ingress {
    description = "Internal VPC traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [aws_vpc.main.cidr_block]
  }

  # App Access from NodePort via ALB
  ingress {
    description = "Traffic from ALB to NodePort 30080"
    from_port   = 30080
    to_port     = 30080
    protocol    = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    description = "Free outbound Internet access"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Security Group for the Application Load Balancer
resource "aws_security_group" "alb" {
  name        = "maps-alb-sg"
  description = "Allow inbound HTTP traffic to the ALB"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP from Internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Outbound traffic from ALB"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Security Group for RDS MySQL
resource "aws_security_group" "rds" {
  name        = "maps-rds-sg"
  description = "Allow inbound traffic to MySQL from K3s nodes"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "EC2 traffic on port 3306"
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
