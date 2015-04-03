require 'java'

def reka
  Java::Reka
end

def dots(str)
  reka.util.Path.dots(str)
end

class DataWrapper
  
  include Enumerable
  
  def initialize(data, base = reka.util.Path.root)
    @data = data
    @base = base
  end
  
  def at(path)
    DataWrapper.new(@data, @base.add(dots(path)))
  end
  
  def each(&block)
    @data.at(@base).for_each_content do |path, content|
      block.call path.dots, content
    end
  end
  
  def [](path)
    @data.getString(@base.add(dots(path))).get
  end
  
  def []=(path, value)
    raise "must be string (for now)" unless value.is_a?(String)
    @data.putString(@base.add(dots(path)), value)
  end
  
end
