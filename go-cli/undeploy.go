package main

import (
  "net/http"
  "fmt"
  "log"
  "os"
)


func main() {

  client := &http.Client{}  

  uuid := os.Args[1]

  url := fmt.Sprintf("http://localhost:5100/apps/%s", uuid)

  req, err := http.NewRequest("DELETE", url, nil) 

  if err != nil {
    log.Fatal(err)
  }

  _, err = client.Do(req)

  fmt.Printf("undeployed %s!\n", uuid)

}
