terraform {
  # Example of Remote Backend using AWS S3
  # backend "s3" {
  #   bucket         = "my-terraform-state-prod"
  #   key            = "ecommerce/prod/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "terraform-locks"
  #   encrypt        = true
  # }

  backend "local" {
    path = "terraform.tfstate"
  }
}
