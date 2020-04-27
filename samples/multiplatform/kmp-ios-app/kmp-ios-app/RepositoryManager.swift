//
//  RepositoryManager.swift
//  kmp-ios-app
//
//  Created by Ellen Shapiro on 4/26/20.
//

import Foundation
import kmp_lib_sample
import SwiftUI

class RepositoryManager: ObservableObject {
    
    private let repository = ApolloiOSRepositoryKt.create()
    
    @Published var repos: [RepositoryFragment] = []
    @Published var repoDetails: [String: RepositoryDetail] = [:]
    @Published var commits: [String: [Any]] = [:]
    
    func fetch() {
        self.repository.fetchRepositories { [weak self] repos in
            self?.repos = repos
        }
    }
    
    func fetchDetails(for repo: RepositoryFragment) {        
        self.repository.fetchRepositoryDetail(repositoryName: repo.name) { [weak self] detail in
            if let detail = detail {
                self?.repoDetails[repo.name] = detail
            }
        }
    }
    
    func fetchCommits(for repo: RepositoryFragment) {
        self.repository.fetchCommits(repositoryName: repo.name) { commits in
            self.commits[repo.name] = commits
        }
    }
}
