import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Table, notification } from "antd";
import { CloseCircleOutlined, CheckCircleOutlined } from '@ant-design/icons';
import axios from "axios";
import Breadcrumbs from "../../../components/Breadcrumbs";
import AddViolation from "../../../components/modelpopup/AddViolation";
import EditViolation from "../../../components/modelpopup/EditViolation";
import { base_url } from "../../../base_urls";
import { Avatar_02 } from "../../../Routes/ImagePath";
const openNotificationWithError = (message) => {
  notification.error({
    message: 'Error',
    description: <span style={{ color: '#ed2d33' }}>{message}</span>,
    placement: 'topRight',
  });
};

const openNotificationWithSuccess = (message) => {
  notification.success({
    message: 'Success',
    description: (
      <div>
        <span style={{ color: '#09b347' }}>{message}</span>
        <button
          onClick={() => notification.destroy()}
          style={{
            border: 'none',
            background: 'transparent',
            float: 'right',
            cursor: 'pointer',
          }}
        >
          <CloseCircleOutlined style={{ color: '#09b347' }} />
        </button>
      </div>
    ),
    placement: 'topRight',
    icon: <CheckCircleOutlined style={{ color: '#52c41a' }} />,
  });
};


const Violation = () => {
  const [violations, setViolations] = useState([]);
  const [deleteId, setDeleteId] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editViolationData, setEditViolationData] = useState(null);
  const [statsData, setStatsData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [userRole, setUserRole] = useState(() =>
    sessionStorage.getItem("userRole")
  );
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchViolations = async () => {
    setLoading(true);
    try {
      const response = await axios.get(`${base_url}/api/violations`, {
        withCredentials: true,
      });
      const sortedViolations = response.data.data.sort((a, b) => new Date(b.violationDate) - new Date(a.violationDate));
      setViolations(sortedViolations);
      await updateStats(sortedViolations);
    } catch (error) {
      openNotificationWithError(`Error fetching violations: ${error.response?.data || error.message}`);
    } finally {
      setLoading(false);
    }
  };
  
  const fetchEmployees = async () => {
    try {
      const response = await axios.get(
        `${base_url}/api/employee/getEmployeesByRole`,
        { withCredentials: true }
      );
      return response.data.data;
    } catch (error) {
      openNotificationWithError(`Error fetching employees: ${error}`);
      return [];
    }
  };

  const updateStats = async (violationsData) => {
    try {
      const allEmployees = await fetchEmployees();
      const currentDate = new Date();
      const currentMonth = currentDate.getMonth(); // Tháng hiện tại (0-11)
      const currentYear = currentDate.getFullYear(); // Năm hiện tại
      const violationsThisMonth = violationsData.filter((v) => { //filter violation this month
        const violationDate = new Date(v.violationDate); // Chuyển đổi chuỗi ngày thành đối tượng Date
        return (
          violationDate.getMonth() === currentMonth &&
          violationDate.getFullYear() === currentYear
        );
      });
      const employeeIdsWithViolations = new Set(
        violationsThisMonth.map((v) => v.employee?.id).filter((id) => id != null)
      );
      const totalEmployeesWithViolations = employeeIdsWithViolations.size;
      const totalViolations = violationsThisMonth.length;
      const pendingViolations = violationsThisMonth.filter(
        (v) => v.status === "Pending"
      ).length;
      const rejectedViolations = violationsThisMonth.filter(
        (v) => v.status === "Rejected"
      ).length;
  
      setStatsData([
        {
          title: "Violation Staff",
          value: totalEmployeesWithViolations, // Nhân viên có vi phạm trong tháng hiện tại
          month: "this month",
        },
        {
          title: "Total Violation",
          value: totalViolations, // Tổng số vi phạm trong tháng hiện tại
          month: "this month",
        },
        {
          title: "Pending Request",
          value: pendingViolations, // Vi phạm đang chờ xử lý trong tháng hiện tại
          month: "this month",
        },
        {
          title: "Rejected",
          value: rejectedViolations, // Vi phạm bị từ chối trong tháng hiện tại
          month: "this month",
        },
      ]);
    } catch (error) {
      openNotificationWithError(`Error updating stats: ${error}`);
    }
  };
  
  useEffect(() => {
    fetchViolations();
  }, []);

  const handleAdd = async (data) => {
    if (isSubmitting) return false; 
  
    setIsSubmitting(true);
  
    try {
      await axios.post(`${base_url}/api/violations`, data, {
        withCredentials: true,
      });
      await fetchViolations(); 
      openNotificationWithSuccess('Violation added successfully.');
      return true; 
    } catch (error) {
      openNotificationWithError(`Error adding violation: ${error.response?.data || error.message}`);
      return false;
    } finally {
      setIsSubmitting(false);
    }
  };
  

  const handleEdit = async (id) => {
    try {
      console.log("Fetching data for violation id:", id);
      const response = await axios.get(`${base_url}/api/violations/${id}`, {
        withCredentials: true,
      });
  
      if (response.data.status === "SUCCESS" && response.data.data) {
        setEditViolationData(response.data); // Đảm bảo rằng dữ liệu đúng
        setShowEditModal(true);
      } else {
        openNotificationWithError(`Invalid violation data: ${response.data.data}`);
      }
    } catch (error) {
      openNotificationWithError(`Error fetching violation data: ${error}`);
    }
  };
  

  const handleSaveEdit = async (data) => {
    if (!data.id) {
      openNotificationWithError("Violation ID is missing");
      return;
    }
  
    try {
      const response = await axios.put(`${base_url}/api/violations/${data.id}`, data, {
        withCredentials: true,
      });
      if (response.data.status === "SUCCESS") {
        fetchViolations();
        openNotificationWithSuccess('Violation updated successfully.');
        setShowEditModal(false);
      } else {
        openNotificationWithError(`Error updating violation: ${response.data.message || "Unknown error"}`);
      }
    } catch (error) {
      openNotificationWithError(`Error updating violation: ${error}`);
    }
  };

  const handleStatusChange = async (violationId, newStatus) => {
    try {
      await axios.put(
        `${base_url}/api/violations/${violationId}/status`,
        null,
        {
          params: { status: newStatus },
          withCredentials: true,
        }
      );
      fetchViolations(); 
    } catch (error) {
      openNotificationWithError(`Error updating status: ${error}`);
    }
  };

  const handleClose = () => {
    setShowDeleteModal(false);
    setShowEditModal(false);
  };

  const userElements = violations.map((item, index) => ({
    key: index,
    id: item.id,
    index: index + 1,
    employeeId: item.id || "Loading...",
    employee: item.employee?.fullname || "Loading...",
    empcode: item.employee?.empCode || "Loading...",
    department: item.employee?.departmentName || "Loading...",
    position: item.employee?.positionName || "Loading...",
    role: item.role || "Loading...",
    description: item.description || "Loading...",
    violationTypeId: item.violationTypeId || "Loading...",
    violationType: item.violationType?.type || "Loading...",
    date: item.violationDate || "Loading...",
    avatar: item.employee?.avatar || Avatar_02,
    status: item.status || "Loading...",
  }));

  const columns = [
    {
      title: "#",
      dataIndex: "index",
      render: (text, record) => <span>{record.index}</span>,
      sorter: (a, b) => a.index - b.index,
    },
    {
      title: "Employee",
      dataIndex: "employee",
      render: (text, record) => (
        <span className="table-avatar">
          <Link to="/client-profile" className="avatar">
            <img alt="" src={record.avatar} />
          </Link>
          <Link to="/client-profile">
            {text} - {record.empcode}
          </Link>
        </span>
      ),
    },
    {
      title: "Department",
      dataIndex: "department",
    },
    {
      title: "Date Violation",
      dataIndex: "date",
      sorter: (a, b) => a.date.length - b.date.length,
    },
    {
      title: "ViolationType",
      dataIndex: "violationType",
    },
    {
      title: "Description",
      dataIndex: "description",
      render: (text) => (
        <span>{text.length > 10 ? `${text.substring(0, 8)}...` : text}</span>
      ),
    },
    {
      title: "Status",
      dataIndex: "status",
      render: (text, record) => (
        <>
          {userRole === "MANAGE" || userRole === "ADMIN" ? (
            <div className="dropdown action-label">
              <button
                className="btn btn-white btn-sm btn-rounded dropdown-toggle"
                type="button"
                id={`dropdownMenuButton-${record.id}`}
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                <i
                  className={
                    text === "Pending"
                      ? "far fa-dot-circle text-danger"
                      : text === "Approved"
                      ? "far fa-dot-circle text-success"
                      : "far fa-dot-circle text-secondary"
                  }
                />{" "}
                {text}
              </button>
              <ul
                className="dropdown-menu"
                aria-labelledby={`dropdownMenuButton-${record.id}`}
              >
                <li>
                  <button
                    className="dropdown-item"
                    onClick={() => handleStatusChange(record.id, "Approved")}
                  >
                    <i className="far fa-dot-circle text-success" /> Approved
                  </button>
                </li>
                <li>
                  <button
                    className="dropdown-item"
                    onClick={() => handleStatusChange(record.id, "Rejected")}
                  >
                    <i className="far fa-dot-circle text-secondary" /> Rejected
                  </button>
                </li>
              </ul>
            </div>
          ) : (
            <span>
              <i
                className={
                  text === "Pending"
                    ? "far fa-dot-circle text-danger"
                    : text === "Approved"
                    ? "far fa-dot-circle text-success"
                    : "far fa-dot-circle text-secondary"
                }
              />{" "}
              {text}
            </span>
          )}
        </>
      ),
    },

    {
      title: "Action",
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
              data-bs-toggle="modal"
              data-bs-target="#edit_violation"
              onClick={() => handleEdit(record.id)} 
            >
              <i className="fa fa-pencil m-r-5" /> Edit
            </Link>
          </div>
        </div>
      ),
    },
  ];

  return (
    <>
      <div className="page-wrapper">
        <div className="content container-fluid">
          {loading ? (
            <div className="text-center w-100">
              <div className="spinner-border" role="status">
                <span className="sr-only">Loading...</span>
              </div>
            </div>
          ) : (
            <>
              <Breadcrumbs
                maintitle="Violation"
                title="Dashboard"
                subtitle="Violation"
                modal="#add_violation"
                name="Add Violation"
              />
              <div className="row">
                {statsData.map((data, index) => (
                  <div
                    className="col-md-6 col-sm-6 col-lg-6 col-xl-3"
                    key={index}
                  >
                    <div className="stats-info">
                      <h6>{data.title}</h6>
                      <h4>
                        {data.value} <small>{data.month}</small>
                      </h4>
                    </div>
                  </div>
                ))}
              </div>
              <div className="row">
                <div className="col-md-12">
                  <div className="table-responsive">
                    {/* <SearchBox /> */}
                    <Table
                      columns={columns}
                      dataSource={userElements}
                      className="table-striped"
                      pagination={{ pageSize: 10 }}
                    />
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
        <AddViolation onAdd={handleAdd} />
        <EditViolation
          show={showEditModal}
          onClose={handleClose}
          onSave={handleSaveEdit}
          violationData={editViolationData}
          userRole={userRole}
        />
      </div>
    </>
  );
};

export default Violation;
