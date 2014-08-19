

puts "I am inside ruby"

data['woah.yeah.this.is.cool'] = 'omg inside ruby'

data['another.deep.path1'] = 'yay222!'

data.at('another')['deep.path3'] = 'boooom'
  
data['another.deep.path'] = "yay!"

data.at('woah.yeah').each do |path, content|
  puts "coolwoahyeah: #{path} -> #{content}"
end

data.each do |path, content|
  puts "each: #{path} -> #{content}"
end

puts "inside ruby? #{data['woah.yeah.this.is.cool']}"

"name is #{data['name']}"