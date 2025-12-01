terraform { 
  cloud { 
    
    organization = "ingesoffttt" 

    workspaces { 
      name = "ecommerce-prod" 
    } 
  } 
}