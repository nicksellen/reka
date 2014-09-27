require 'rubygems'
require 'nokogiri'
require 'open-uri'

# text = open('http://nicksellen.co.uk').read

text = '<h1>Offline</h1>'

doc = Nokogiri(text)
  
puts "got text on website #{doc.at('h1').text}"