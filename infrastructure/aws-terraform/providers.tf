terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # -------------------------------------------------------------------------
  # REMOTE S3 BACKEND (Production-Ready)
  # Uncomment this block to store the State in a shared cloud bucket.
  # Ensure the "maps-app-terraform-state-bucket" bucket and
  # "terraform-state-locks" DynamoDB table are manually created on AWS first.
  # -------------------------------------------------------------------------
  # backend "s3" {
  #   bucket         = "maps-app-terraform-state-bucket"
  #   key            = "global/s3/terraform.tfstate"
  #   region         = "eu-west-1"
  #   dynamodb_table = "terraform-state-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region
}
