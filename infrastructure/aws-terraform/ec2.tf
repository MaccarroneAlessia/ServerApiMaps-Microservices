# AWS COMPUTE INFRASTRUCTURE (EC2)

# Provisions the virtual machines where Kubernetes (K3s) will run.
# Dynamically creates SSH keys and defines the two nodes:
# - k3s_master (t3.small): Manages the cluster control plane.
# - k3s_worker (t3.micro): Physically runs the Spring Boot container.
# Uses local_file to locally save keys and IPs, making them available for Ansible.

data "aws_ami" "ubuntu" {
  most_recent = true
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
  owners = ["099720109477"] # Canonical
}

# Dynamically generate SSH key for Ansible
resource "tls_private_key" "ssh_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "generated_key" {
  key_name   = "maps-k3s-key"
  public_key = tls_private_key.ssh_key.public_key_openssh
}

resource "aws_instance" "k3s_master" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t3.small"
  subnet_id     = aws_subnet.public_1.id
  vpc_security_group_ids = [aws_security_group.k3s_nodes.id]
  key_name      = aws_key_pair.generated_key.key_name
  associate_public_ip_address = true

  tags = {
    Name = "maps-k3s-master"
  }
}

resource "aws_instance" "k3s_worker" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t3.micro"
  subnet_id     = aws_subnet.public_2.id
  vpc_security_group_ids = [aws_security_group.k3s_nodes.id]
  key_name      = aws_key_pair.generated_key.key_name
  associate_public_ip_address = true

  tags = {
    Name = "maps-k3s-worker"
  }
}

# Generate Ansible inventory file based on the template
resource "local_file" "ansible_inventory" {
  content = templatefile("${path.module}/../ansible/inventory.tmpl", {
    master_ip = aws_instance.k3s_master.public_ip
    worker_ip = aws_instance.k3s_worker.public_ip
  })
  filename = "${path.module}/../ansible/inventory.ini"
}

# Save SSH key locally (ignored by git) for Ansible and GitHub Actions to use
resource "local_file" "private_key" {
  content  = tls_private_key.ssh_key.private_key_pem
  filename = "${path.module}/../ansible/k3s-key.pem"
  file_permission = "0600"
}
