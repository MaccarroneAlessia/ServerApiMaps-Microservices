# AWS NETWORK INFRASTRUCTURE (VPC)

# Defines network isolation (crucial for cloud security)
# Creates a Virtual Private Cloud (VPC) containing:
# - Public Subnets: For resources exposed to the internet (e.g., ALB)
# - Private Subnets: For isolated resources (e.g., RDS Database)
# - Internet Gateway & Route Tables: To allow outbound traffic.

# Main VPC
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "maps-vpc"
  }
}

# Internet Gateway for internet access
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "maps-igw"
  }
}

# Public Subnets (For ALB and ECS Fargate)
# Note: ECS is placed in a public subnet to avoid expensive NAT Gateway costs (around $30/month),
# while maintaining security through Security Groups.
resource "aws_subnet" "public_1" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = true

  tags = {
    Name                                 = "maps-public-1"
    "kubernetes.io/cluster/maps-cluster" = "shared"
    "kubernetes.io/role/elb"             = "1"
  }
}

resource "aws_subnet" "public_2" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.aws_region}b"
  map_public_ip_on_launch = true

  tags = {
    Name                                 = "maps-public-2"
    "kubernetes.io/cluster/maps-cluster" = "shared"
    "kubernetes.io/role/elb"             = "1"
  }
}

# Private Subnets (For RDS)
resource "aws_subnet" "private_1" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = "${var.aws_region}a"

  tags = {
    Name                                 = "maps-private-1"
    "kubernetes.io/cluster/maps-cluster" = "shared"
    "kubernetes.io/role/internal-elb"    = "1"
  }
}

resource "aws_subnet" "private_2" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.4.0/24"
  availability_zone = "${var.aws_region}b"

  tags = {
    Name                                 = "maps-private-2"
    "kubernetes.io/cluster/maps-cluster" = "shared"
    "kubernetes.io/role/internal-elb"    = "1"
  }
}

# Routing table for public subnets
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "maps-public-rt"
  }
}

resource "aws_route_table_association" "public_1" {
  subnet_id      = aws_subnet.public_1.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "public_2" {
  subnet_id      = aws_subnet.public_2.id
  route_table_id = aws_route_table.public.id
}
