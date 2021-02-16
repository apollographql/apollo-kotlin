//
//  ContentView.swift
//  kmp-ios-app
//
//  Created by Taso Dane on 4/5/20.
//

import SwiftUI
import kmp_lib_sample

struct RepoListView: View {
    
    @ObservedObject var repoManager: RepositoryManager
    
    var body: some View {
        NavigationView {
            List(repoManager.repos) { repo in
                NavigationLink(destination:
                    RepoDetailView(repo: repo, repoManager: self.repoManager)
                        .onAppear {
                            self.repoManager.fetchDetails(for: repo)
                        }
                ) {
                    RepositoryCell(repo: repo)
                }
                
            }.navigationBarTitle("Repositories")
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        let repos = [
            RepositoryFragmentImpl.Data(__typename: "__typename", id: "1", name: "Test1", repoDescription: "Test Repo 1"),
            RepositoryFragmentImpl.Data(__typename: "__typename", id: "2", name: "Test2", repoDescription: "Test Repo 2")
        ]
        
        let repoManager = RepositoryManager()
        repoManager.repos = repos
        return RepoListView(repoManager: repoManager)
    }
}

extension RepositoryFragmentImpl: Identifiable { }
