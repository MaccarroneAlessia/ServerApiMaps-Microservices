output "alb_dns_name" {
  description = "Il DNS pubblico dell'Application Load Balancer per accedere all'app"
  value       = aws_lb.main.dns_name
}

output "ecr_repository_uri" {
  description = "URI del repository ECR per pushare le immagini Docker"
  value       = aws_ecr_repository.app.repository_url
}

output "k3s_master_ip" {
  description = "IP Pubblico del nodo Master K3s"
  value       = aws_instance.k3s_master.public_ip
}

output "k3s_worker_ip" {
  description = "IP Pubblico del nodo Worker K3s"
  value       = aws_instance.k3s_worker.public_ip
}
