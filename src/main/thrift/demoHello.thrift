namespace java model

struct Model{
1:i32 id;
2:string name;
}

service  HelloWorldService {
  string sayHello(1:string username)
  Model returnModel(1:string name)
  string modelProcess(1:Model model)
}