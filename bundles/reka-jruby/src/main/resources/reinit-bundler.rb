gem 'bundler', '>= 0'

require 'bundler'

require 'bundler/friendly_errors'
Bundler.with_friendly_errors do
  require 'bundler/cli'
  Bundler::CLI.start %W[install --path=#{ENV['GEM_PATH']}], :debug => true
end
