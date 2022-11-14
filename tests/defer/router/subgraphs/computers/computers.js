const {ApolloServer, gql} = require('apollo-server');
const {readFileSync} = require('fs');

const port = process.env.APOLLO_PORT || 4000;

const computers = [
    {id: 'Computer1', cpu: "386", year: 1993, screen: {resolution: "640x480", isColor: false}},
    {id: 'Computer2', cpu: "486", year: 1996, screen: {resolution: "800x600", isColor: true}},
]

const typeDefs = gql(readFileSync('./computers.graphqls', {encoding: 'utf-8'}));
const resolvers = {
    Query: {
        computers: (_, args, context) => {
            return computers;
        },
        computer: (_, args, context) => {
            return computers.find(p => p.id === args.id);
        }
    },
    Mutation: {
        computers: (_, args, context) => {
            return computers;
        }
    },
    Computer: {
        errorField: (_, args, context) => {
            throw new Error("Error field");
        },
        nonNullErrorField: (_, args, context) => {
            return null;
        }
    }
}
const server = new ApolloServer({typeDefs, resolvers});
server.listen({port: port}).then(({url}) => {
    console.log(`🚀 Computers subgraph ready at ${url}`);
}).catch(err => {
    console.error(err)
});
