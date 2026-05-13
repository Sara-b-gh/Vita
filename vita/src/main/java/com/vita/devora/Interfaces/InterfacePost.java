package com.vita.devora.Interfaces;

import com.vita.devora.Entities.CommentDisplay;
import com.vita.devora.Entities.Post;
import com.vita.devora.Entities.PostView;
import javafx.geometry.Pos;

import java.sql.SQLException;
import java.util.List;

public interface InterfacePost<P> {

    void AddPost(P post) throws SQLException;
    void UpdatePost(P post) throws SQLException;
    void DeletePost(int id) throws SQLException;
    List<P> GetAllPost() throws SQLException;
    P GetPostById(int id) throws SQLException;

    List<PostView> getAllPostsSortedByDateDesc();
    List<PostView> filterByCategory(Post.Category category);


}