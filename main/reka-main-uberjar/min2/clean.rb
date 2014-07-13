require 'set'

keep = ARGV.shift

unless keep
  puts "Usage: #{$0} <keeplist>"
  exit 0
end

keep_classnames = Set.new(open(keep).read.split("\n"))

ARGF.readlines.each do |filename|
  next unless filename =~ /\.class$/
  classname = filename.
    gsub(/\.class$/, '').
    gsub(/^\.\//, '').
    gsub(/\//, '.').
    gsub(/\$.+$/, '')

  next if keep_classnames.include? classname

  puts filename
end
