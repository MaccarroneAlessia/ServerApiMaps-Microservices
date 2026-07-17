# AWS APPLICATION LOAD BALANCER (ALB)

# The ALB acts as a public "Proxy". Instead of directly exposing the EC2 machines
# to internet clients, the ALB receives HTTP traffic on port 80 and securely routes it
# to the nodes on port 30080 (the NodePort exposed by K3s).
# It also defines a Target Group to monitor the health of the EC2 nodes.

resource "aws_lb" "main" {
  name               = "maps-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [aws_subnet.public_1.id, aws_subnet.public_2.id]
}

resource "aws_lb_target_group" "app" {
  name        = "maps-tg"
  port        = 30080 # K3s NodePort
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
