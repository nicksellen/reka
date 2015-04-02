if RUBY_VERSION =~ /^1\.9\.1/ && defined?(Gem::QuickLoader)
  Gem::QuickLoader.load_full_rubygems_library
end

require 'rubygems'
require 'rubygems/gem_runner'
require 'rubygems/exceptions'

begin
  Gem::GemRunner.new.run %W[install bundler --install-dir #{ENV['GEM_PATH']}]
rescue Gem::SystemExitException => e
  # don't exit yet!
end