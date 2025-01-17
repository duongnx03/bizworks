import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Table, message, Button, Input } from "antd";
import axios from "axios";
import Breadcrumbs from "../../../components/Breadcrumbs";
import DeleteModal from "../../../components/modelpopup/DeleteModal";
import SearchBox from "../../../components/SearchBox";
import DepartmentModal from "../../../components/modelpopup/DepartmentModal";
import { base_url } from "../../../base_urls";

const Department = () => {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [departmentToDelete, setDepartmentToDelete] = useState(null);
  const [departmentModalOpen, setDepartmentModalOpen] = useState(false);
  const [selectedDepartment, setSelectedDepartment] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    fetchDepartments();
  }, []);

  const fetchDepartments = async (search = "") => {
    setLoading(true);
    try {
      const response = await axios.get(`${base_url}/api/departments`, { withCredentials: true });
      console.log(response.data);
      if (Array.isArray(response.data)) {
        const filteredDepartments = response.data.filter(department =>
          department.departmentName.toLowerCase().includes(search.toLowerCase()) // Sửa thành departmentName
        );
        setDepartments(filteredDepartments);
      } else {
        message.error("Invalid data format received from API");
        setDepartments([]);
      }
    } catch (error) {
      message.error("Failed to fetch departments"); // Thông báo lỗi
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (value) => {
    setSearchTerm(value);
    fetchDepartments(value);
  };

  const handleDepartmentCreated = () => {
    fetchDepartments();
    setDepartmentModalOpen(false);
    setSelectedDepartment(null);
  };

  const openDeleteModal = (id) => {
    setDepartmentToDelete(id);
    setDeleteModalOpen(true);
  };

  const handleDeleteModalClose = () => {
    setDeleteModalOpen(false);
    setDepartmentToDelete(null);
  };

  const handleDelete = async () => {
    try {
      await axios.delete(`${base_url}/api/departments/${departmentToDelete}`, { withCredentials: true });
      setDepartments(departments.filter(department => department.id !== departmentToDelete));
      handleDeleteModalClose();
      message.success("Department deleted successfully"); // Thông báo thành công
    } catch (error) {
      if (error.response && error.response.status === 403) {
        message.error("Access denied. Please log in.");
      } else {
        message.error("Failed to delete department");
      }
    }
  };

  const departmentElements = Array.isArray(departments) ? departments.map((department) => ({
    key: department.id,
    id: department.id,
    department: department.departmentName, // Sửa thành departmentName
  })) : [];

  const columns = [
    {
      title: "ID",
      dataIndex: "id",
      sorter: (a, b) => a.id - b.id,
      width: "10%",
    },
    {
      title: "Department Name",
      dataIndex: "department",
      sorter: (a, b) => a.department.localeCompare(b.department),
      width: "50%",
    },
    {
      title: "Actions",
      className: "text-end",
      render: (text, record) => (
        <div className="dropdown dropdown-action text-end">
          <Link
            to="#"
            className="action-icon dropdown-toggle"
            data-bs-toggle="dropdown"
            aria-expanded="false"
          >
            <i className="material-icons">more_vert</i>
          </Link>
          <div className="dropdown-menu dropdown-menu-right">
            <Link
              className="dropdown-item"
              to="#"
              onClick={() => {
                setSelectedDepartment(record);
                setDepartmentModalOpen(true);
              }}
            >
              <i className="fa fa-pencil m-r-5" /> Edit
            </Link>
            <Link
              className="dropdown-item"
              to="#"
              onClick={() => openDeleteModal(record.id)}>
              <i className="fa fa-trash m-r-5" /> Delete
            </Link>
            <Link
              className="dropdown-item"
              to={`/positions/${record.id}`}>
              <i className="fa fa-eye m-r-5" /> View Positions
            </Link>
          </div>
        </div>
      ),
      width: "40%",
    },
  ];

  return (
    <>
      <div className="page-wrapper">
        <div className="content container-fluid">
          <Breadcrumbs
            maintitle="Department"
            title="Dashboard"
            subtitle="Department"
            modal="#add_department"
            name="Add Department"
          />
          <div className="row mb-3">
            <div className="col-md-12 text-end">
              <Input 
                placeholder="Tìm kiếm theo tên phòng ban" 
                value={searchTerm} 
                onChange={e => handleSearch(e.target.value)} 
                style={{ width: '300px' }} 
              />
            </div>
          </div>
          <div className="row">
            <div className="col-md-12">
              <div className="table-responsive">
                <SearchBox />
                <Table
                  columns={columns}
                  dataSource={departmentElements}
                  loading={loading}
                  className="table-striped"
                  rowKey="id"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <DepartmentModal 
        visible={departmentModalOpen} 
        onDepartmentCreated={handleDepartmentCreated} 
        onClose={() => setDepartmentModalOpen(false)} 
        department={selectedDepartment} 
      />
      {deleteModalOpen && (
        <DeleteModal
          id={departmentToDelete}
          Name="Delete Department"
          onDelete={handleDelete}
          onClose={handleDeleteModalClose}
        />
      )}
    </>
  );
};

export default Department;
