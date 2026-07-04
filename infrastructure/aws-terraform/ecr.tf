resource "aws_ecr_repository" "app" {
  name                 = "maps-app-repo"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}
