# AWS APPLICATION LOAD BALANCER (ALB)

# L'ALB fa da "Proxy" pubblico. Invece di esporre direttamente le macchine EC2
# ai client su Internet, l'ALB riceve traffico HTTP sulla porta 80 e lo instrada 
# in modo sicuro verso i nodi sulla porta 30080 (il NodePort esposto da K3s).
# Definisce anche un Target Group per monitorare la salute dei nodi EC2.

resource "aws_lb" "main" {
  name               = "maps-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [aws_subnet.public_1.id, aws_subnet.public_2.id]
}

resource "aws_lb_target_group" "app" {
  name        = "maps-tg"
  port        = 30080 # NodePort di K3s
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "instance"

  health_check {
    healthy_threshold   = 3
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    path                = "/actuator/health"
    matcher             = "200-399"
  }
}

resource "aws_lb_target_group_attachment" "master" {
  target_group_arn = aws_lb_target_group.app.arn
  target_id        = aws_instance.k3s_master.id
  port             = 30080
}

resource "aws_lb_target_group_attachment" "worker" {
  target_group_arn = aws_lb_target_group.app.arn
  target_id        = aws_instance.k3s_worker.id
  port             = 30080
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}
