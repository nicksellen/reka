package config

import (
  "encoding/json"
  "io/ioutil"
)

type CLIConfig struct {
  URL  string `json:"url"`
}

func Load(filename string) (CLIConfig, error) {
  var config CLIConfig
  var data, err = ioutil.ReadFile(filename)
  if err != nil {
    return config, err
  }
  json.Unmarshal(data, &config)
  return config, nil
}