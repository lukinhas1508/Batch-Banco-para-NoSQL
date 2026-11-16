// Executado pelo container do MongoDB na primeira inicialização, no banco definido em MONGO_INITDB_DATABASE.
// Cria a coleção de destino e os índices usados nas consultas mais comuns.
db.createCollection("clientes");
db.clientes.createIndex({ email: 1 }, { unique: true });
db.clientes.createIndex({ "endereco.estado": 1, "endereco.cidade": 1 });
